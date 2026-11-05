package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BookTypeEntity
import kotlinx.coroutines.flow.Flow

interface BookTypeRepository {
    fun getAllBookTypes(): Flow<List<BookTypeEntity>>
    suspend fun getAllBookTypesOnce(): List<BookTypeEntity>
    suspend fun getBookTypeByName(name: String): BookTypeEntity?

    /** 创建类型，重名时返回已有记录的 id */
    suspend fun createBookType(name: String): Long

    /** 重命名类型，同时同步更新所有关联书籍；重名或名称为空返回 false */
    suspend fun renameBookType(id: Long, newName: String): Boolean

    /**
     * 删除类型，关联书籍回退到剩余中最早创建的类型。
     * 仅剩一个类型时拒绝删除，返回 false。
     */
    suspend fun deleteBookType(id: Long): Boolean

    suspend fun countBooksWithType(name: String): Int

    /** 确保默认类型存在，缺失时补建（用于数据库迁移后兜底） */
    suspend fun ensureDefaultsExist()
}
