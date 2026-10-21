package com.readtrack.domain.repository

import com.readtrack.data.local.entity.ReadingRecordEntity
import kotlinx.coroutines.flow.Flow

interface ReadingRecordRepository {
    fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>>
    fun getAllRecords(): Flow<List<ReadingRecordEntity>>
    fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>>
    fun getRecordsByDateRange(start: Long, end: Long): Flow<List<ReadingRecordEntity>>
    suspend fun hasNormalRecordSince(startTime: Long): Boolean
    suspend fun insertRecord(record: ReadingRecordEntity): Long
    suspend fun deleteRecord(record: ReadingRecordEntity)
    /** 查询指定书籍在指定日期范围内的最新一条 NORMAL 记录（用于同日 upsert） */
    suspend fun getLatestNormalRecordToday(bookId: Long, dayStart: Long, dayEnd: Long): ReadingRecordEntity?
}
