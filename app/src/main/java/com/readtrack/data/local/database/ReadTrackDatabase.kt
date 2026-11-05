package com.readtrack.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.readtrack.data.local.dao.BadgeDao
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.BookTypeDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.dao.TagDao
import com.readtrack.data.local.entity.BadgeEntity
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.BookTypeEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.TagCrossRef
import com.readtrack.data.local.entity.TagEntity

@Database(
    entities = [
        BookEntity::class,
        ReadingRecordEntity::class,
        BookListEntity::class,
        BookListCrossRef::class,
        TagEntity::class,
        TagCrossRef::class,
        BadgeEntity::class,
        BookTypeEntity::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReadTrackDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingRecordDao(): ReadingRecordDao
    abstract fun bookListDao(): BookListDao
    abstract fun tagDao(): TagDao
    abstract fun badgeDao(): BadgeDao
    abstract fun bookTypeDao(): BookTypeDao
}
