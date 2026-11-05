package com.readtrack.data.repository

import androidx.room.withTransaction
import com.readtrack.data.local.dao.BookTypeDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.entity.BookTypeEntity
import com.readtrack.domain.repository.BookTypeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookTypeRepositoryImpl @Inject constructor(
    private val bookTypeDao: BookTypeDao,
    private val database: ReadTrackDatabase
) : BookTypeRepository {

    override fun getAllBookTypes(): Flow<List<BookTypeEntity>> =
        bookTypeDao.getAllBookTypes()

    override suspend fun getAllBookTypesOnce(): List<BookTypeEntity> =
        bookTypeDao.getAllBookTypesOnce()

    override suspend fun getBookTypeByName(name: String): BookTypeEntity? =
        bookTypeDao.getBookTypeByName(name)

    override suspend fun createBookType(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        bookTypeDao.getBookTypeByName(trimmed)?.let { return it.id }
        return bookTypeDao.insertBookType(BookTypeEntity(name = trimmed))
    }

    override suspend fun renameBookType(id: Long, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val target = bookTypeDao.getBookTypeById(id) ?: return false
        if (target.name == trimmed) return true

        // 重名则拒绝，避免唯一索引冲突
        bookTypeDao.getBookTypeByName(trimmed)?.let { return false }

        // 改书籍与改类型表必须原子完成，否则中途失败会产生孤儿类型名
        database.withTransaction {
            bookTypeDao.reassignBooks(target.name, trimmed)
            bookTypeDao.renameBookType(id, trimmed)
        }
        return true
    }

    override suspend fun deleteBookType(id: Long): Boolean {
        val all = bookTypeDao.getAllBookTypesOnce()
        val target = all.firstOrNull { it.id == id } ?: return true

        // 至少要保留一个类型，否则书籍会失去类型归属
        val fallback = all.filter { it.id != id }.minByOrNull { it.createdAt } ?: return false

        database.withTransaction {
            bookTypeDao.reassignBooks(target.name, fallback.name)
            bookTypeDao.deleteBookTypeById(id)
        }
        return true
    }

    override suspend fun countBooksWithType(name: String): Int =
        bookTypeDao.countBooksWithType(name)

    /**
     * 仅在类型表为空时补建内置类型。
     *
     * 用户已把内置类型当作普通类型对待，可以删除；若无条件补建，
     * 删掉的「小说」会在下次进入添加书籍页时自动复活。
     */
    override suspend fun ensureDefaultsExist() {
        if (bookTypeDao.getAllBookTypesOnce().isNotEmpty()) return
        DEFAULT_TYPE_NAMES.forEach { name ->
            runCatching { bookTypeDao.insertBookType(BookTypeEntity(name = name)) }
        }
    }

    companion object {
        /** 迁移时写入的内置类型名称，顺序即展示顺序 */
        val DEFAULT_TYPE_NAMES = listOf("小说", "漫画", "有声书")
    }
}
