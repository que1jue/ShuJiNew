package com.readtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.readtrack.data.local.entity.BookTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookTypeDao {

    @Query("SELECT * FROM book_types ORDER BY createdAt ASC, id ASC")
    fun getAllBookTypes(): Flow<List<BookTypeEntity>>

    @Query("SELECT * FROM book_types ORDER BY createdAt ASC, id ASC")
    suspend fun getAllBookTypesOnce(): List<BookTypeEntity>

    @Query("SELECT * FROM book_types WHERE name = :name LIMIT 1")
    suspend fun getBookTypeByName(name: String): BookTypeEntity?

    @Query("SELECT * FROM book_types WHERE id = :id LIMIT 1")
    suspend fun getBookTypeById(id: Long): BookTypeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBookType(bookType: BookTypeEntity): Long

    @Query("UPDATE book_types SET name = :newName WHERE id = :id")
    suspend fun renameBookType(id: Long, newName: String)

    @Query("DELETE FROM book_types WHERE id = :id")
    suspend fun deleteBookTypeById(id: Long)

    /** 统计使用某类型的书籍数量，用于删除前的影响提示 */
    @Query("SELECT COUNT(*) FROM books WHERE bookType = :name")
    suspend fun countBooksWithType(name: String): Int

    /** 删除类型时，把关联书籍回退到默认类型 */
    @Query("UPDATE books SET bookType = :fallbackName WHERE bookType = :oldName")
    suspend fun reassignBooks(oldName: String, fallbackName: String)
}
