package com.readtrack.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.readtrack.data.local.dao.BadgeDao
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.BookTypeDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.dao.TagDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.repository.BadgeRepositoryImpl
import com.readtrack.data.repository.BookListRepositoryImpl
import com.readtrack.data.repository.BookRepositoryImpl
import com.readtrack.data.repository.BookTypeRepositoryImpl
import com.readtrack.data.repository.DataBackupRepositoryImpl
import com.readtrack.data.repository.ReadingRecordRepositoryImpl
import com.readtrack.data.repository.TagRepositoryImpl
import com.readtrack.domain.badge.BadgeCheckScheduler
import com.readtrack.domain.repository.BadgeRepository
import com.readtrack.domain.repository.BookListRepository
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.BookTypeRepository
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.domain.repository.TagRepository
import com.readtrack.util.CoverStorageUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReadTrackDatabase {
        return Room.databaseBuilder(
            context,
            ReadTrackDatabase::class.java,
            "readtrack_database"
        )
            .addMigrations(
                MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
                MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * 从版本 9 迁移到 10：为 books 表新增 bookType 列（默认 'NOVEL'）
     */
    private val MIGRATION_9_10 = Migration(9, 10) { db ->
        db.execSQL("ALTER TABLE books ADD COLUMN bookType TEXT NOT NULL DEFAULT 'NOVEL'")
    }

    /**
     * 从版本 10 迁移到 11：新增 tags 表和 book_tag_cross_ref 表
     */
    private val MIGRATION_10_11 = Migration(10, 11) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color INTEGER,
                createdAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS book_tag_cross_ref (
                tagId INTEGER NOT NULL,
                bookId INTEGER NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY (tagId, bookId),
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE,
                FOREIGN KEY (bookId) REFERENCES books(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_tagId ON book_tag_cross_ref(tagId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_bookId ON book_tag_cross_ref(bookId)")
    }

    /**
     * 从版本 11 迁移到 12：重建 tags.name 索引为 UNIQUE，防止同名标签
     */
    private val MIGRATION_11_12 = Migration(11, 12) { db ->
        db.execSQL("DROP INDEX IF EXISTS index_tags_name")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
    }

    /**
     * 从版本 12 迁移到 13：为 book_lists 表新增 showInBooksPage 列（默认 1/true）
     */
    private val MIGRATION_12_13 = Migration(12, 13) { db ->
        db.execSQL("ALTER TABLE book_lists ADD COLUMN showInBooksPage INTEGER NOT NULL DEFAULT 1")
    }

    /**
     * 从版本 13 迁移到 14：新增 badges 表存储已获得的成就徽章
     */
    private val MIGRATION_13_14 = Migration(13, 14) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS badges (
                id TEXT PRIMARY KEY NOT NULL,
                earnedAt INTEGER NOT NULL
            )
        """)
    }

    /**
     * 从版本 14 迁移到 15：新增 book_types 表，书籍类型由固定枚举改为用户可自定义
     *
     * 关键点：
     * 1. books.bookType 原本存枚举名（NOVEL/COMIC/AUDIOBOOK），需就地改写为中文显示名，
     *    因为 BookEntity.bookType 的类型由 BookType 枚举改为 String
     * 2. 三个内置类型作为普通行插入，createdAt 统一为 0，靠自增 id 保证展示顺序稳定
     * 3. 索引名必须是 index_book_types_name，与 Room 依据实体定义生成的名称一致
     */
    private val MIGRATION_14_15 = Migration(14, 15) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS book_types (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_book_types_name ON book_types(name)")

        // 按固定顺序插入内置类型（小说、漫画、有声书）
        db.execSQL("INSERT OR IGNORE INTO book_types (name, createdAt) VALUES ('小说', 0)")
        db.execSQL("INSERT OR IGNORE INTO book_types (name, createdAt) VALUES ('漫画', 0)")
        db.execSQL("INSERT OR IGNORE INTO book_types (name, createdAt) VALUES ('有声书', 0)")

        // 把 books.bookType 从枚举名改写为中文名
        db.execSQL("UPDATE books SET bookType = '小说' WHERE bookType = 'NOVEL'")
        db.execSQL("UPDATE books SET bookType = '漫画' WHERE bookType = 'COMIC'")
        db.execSQL("UPDATE books SET bookType = '有声书' WHERE bookType = 'AUDIOBOOK'")
        // 兜底：历史脏数据统一归到"小说"，避免出现指向不存在类型的孤儿值
        db.execSQL("UPDATE books SET bookType = '小说' WHERE bookType NOT IN (SELECT name FROM book_types)")
    }

    @Provides
    @Singleton
    fun provideBookDao(database: ReadTrackDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton
    fun provideReadingRecordDao(database: ReadTrackDatabase): ReadingRecordDao {
        return database.readingRecordDao()
    }

    @Provides
    @Singleton
    fun provideBookListDao(database: ReadTrackDatabase): BookListDao {
        return database.bookListDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: ReadTrackDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideBadgeDao(database: ReadTrackDatabase): BadgeDao {
        return database.badgeDao()
    }

    @Provides
    @Singleton
    fun provideBookTypeDao(database: ReadTrackDatabase): BookTypeDao {
        return database.bookTypeDao()
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao,
        database: ReadTrackDatabase,
        badgeCheckScheduler: BadgeCheckScheduler
    ): BookRepository {
        return BookRepositoryImpl(bookDao, readingRecordDao, database, badgeCheckScheduler)
    }

    @Provides
    @Singleton
    fun provideBookListRepository(
        bookListDao: BookListDao,
        bookDao: BookDao,
        database: ReadTrackDatabase
    ): BookListRepository {
        return BookListRepositoryImpl(bookListDao, bookDao, database)
    }

    @Provides
    @Singleton
    fun provideReadingRecordRepository(
        readingRecordDao: ReadingRecordDao,
        badgeCheckScheduler: BadgeCheckScheduler
    ): ReadingRecordRepository {
        return ReadingRecordRepositoryImpl(readingRecordDao, badgeCheckScheduler)
    }

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository {
        return TagRepositoryImpl(tagDao)
    }

    @Provides
    @Singleton
    fun provideBookTypeRepository(
        bookTypeDao: BookTypeDao,
        database: ReadTrackDatabase
    ): BookTypeRepository {
        return BookTypeRepositoryImpl(bookTypeDao, database)
    }

    @Provides
    @Singleton
    fun provideBadgeRepository(
        badgeDao: BadgeDao,
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao
    ): BadgeRepository {
        return BadgeRepositoryImpl(badgeDao, bookDao, readingRecordDao)
    }

    @Provides
    @Singleton
    fun provideDataBackupRepository(
        @ApplicationContext context: Context,
        database: ReadTrackDatabase,
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao,
        bookListDao: BookListDao,
        tagDao: TagDao,
        preferencesManager: PreferencesManager,
        coverStorageUtil: CoverStorageUtil,
        tagRepository: TagRepository,
        bookTypeRepository: BookTypeRepository,
        badgeDao: BadgeDao,
        badgeRepository: BadgeRepository
    ): DataBackupRepository {
        return DataBackupRepositoryImpl(
            context = context,
            database = database,
            bookDao = bookDao,
            recordDao = readingRecordDao,
            bookListDao = bookListDao,
            tagDao = tagDao,
            preferencesManager = preferencesManager,
            coverStorageUtil = coverStorageUtil,
            tagRepository = tagRepository,
            bookTypeRepository = bookTypeRepository,
            badgeDao = badgeDao,
            badgeRepository = badgeRepository
        )
    }
}
