package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书籍类型。用户可自行创建、重命名、删除。
 * 名称唯一，BookEntity.bookType 以名称字符串关联本表。
 */
@Entity(
    tableName = "book_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class BookTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
