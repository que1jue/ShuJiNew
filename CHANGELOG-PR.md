# PR 变更文档：进度类型独立切换 + 同日记录 Upsert + 对话框刷新修复

## 概述

本次 PR 包含三项功能改进：

1. **进度类型独立切换**：用户可在添加书籍页面和书籍详情页手动切换"页数/章节数"进度单位，不再完全依赖全局设置
2. **同日记录 Upsert**：同一天多次更新阅读进度时，自动覆盖当日已有记录而非重复插入
3. **对话框刷新修复**：更新进度对话框每次打开时正确重置输入状态

---

## 文件变更详情（共 7 个文件）

> **注意**：以下文件路径均相对于项目根目录 `app/src/main/java/com/readtrack/`

---

### 文件 1：`data/local/PreferencesManager.kt`

#### 修改 1.1 — 新增导入（第 21 行）

在 `import com.readtrack.domain.model.PreferencesExport` 之后插入：

```kotlin
import com.readtrack.domain.model.ProgressType
```

#### 修改 1.2 — 文件末尾追加扩展函数（第 455-460 行）

在 `PreferencesManager` 类的闭合大括号 `}` 之后（即类外部），追加：

```kotlin

/** 将统计设置中的 StatsUnit 映射为书籍的 ProgressType */
fun StatsUnit.toProgressType(): ProgressType = when (this) {
    StatsUnit.CHAPTER -> ProgressType.CHAPTER
    StatsUnit.PAGE    -> ProgressType.PAGE
}
```

**说明**：将全局设置中的 `StatsUnit` 枚举映射为书籍模型中的 `ProgressType` 枚举，供新建书籍时使用。

---

### 文件 2：`presentation/ui/addbook/AddBookScreen.kt`

#### 修改 2.1 — 插入进度类型选择器（第 398-425 行）

在 `// Progress Fields (auto-determined by settings)` 注释**之前**（即 `OutlinedTextField` "出版社"输入框之后），插入以下代码：

```kotlin
            // 进度类型选择（页数 / 章节数）
            Text(
                "进度单位",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.progressType == type,
                        onClick = { viewModel.updateProgressType(type) },
                        label = {
                            Text(
                                when (type) {
                                    ProgressType.PAGE    -> "页数"
                                    ProgressType.CHAPTER -> "章节数"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

```

**说明**：在添加书籍界面的进度输入区域前增加 FilterChip 选择器，用户可手动切换"页数"或"章节数"。

---

### 文件 3：`presentation/ui/books/BookDetailScreen.kt`

#### 修改 3.1 — 插入进度类型切换器（第 131-169 行）

在 `LazyColumn` 中 `BookInfoCard` item 之后、`ProgressInfoCard` item 之前，插入新的 `item {}`：

```kotlin
                    // 进度类型切换
                    item {
                        Column {
                            Text(
                                "进度单位",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProgressType.entries.forEach { type ->
                                    FilterChip(
                                        selected = book.progressType == type,
                                        onClick = { viewModel.updateProgressType(type) },
                                        label = {
                                            Text(
                                                when (type) {
                                                    ProgressType.PAGE    -> "页数"
                                                    ProgressType.CHAPTER -> "章节数"
                                                },
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "提示：每本书可独立设置进度单位，已创建的书不会随全局设置自动变更，请在此处手动调整",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
```

**说明**：在书籍详情页头部卡片和进度卡片之间添加进度类型切换器，附带提示文字。

#### 修改 3.2 — 对话框状态重置 key 包裹（第 443 行）

找到更新进度对话框中 `var inputText` 和 `var isIncrement` 两行 `remember`，在其外层包裹 `key(book.id, showAddRecordDialog) { ... }`。

**修改前**（原始代码）：
```kotlin
    if (showAddRecordDialog && uiState.book != null) {
        val book = uiState.book!!
        var inputText by remember { mutableStateOf("") }
        var isIncrement by remember { mutableStateOf(true) }
        val isChapterBased = book.progressType == ProgressType.CHAPTER
        
        AlertDialog(
            // ... 对话框内容 ...
            dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("取消") } }
        )
    }
```

**修改后**：
```kotlin
    if (showAddRecordDialog && uiState.book != null) {
        val book = uiState.book!!
        key(book.id, showAddRecordDialog) {
            var inputText by remember { mutableStateOf("") }
            var isIncrement by remember { mutableStateOf(true) }
            val isChapterBased = book.progressType == ProgressType.CHAPTER
            
            AlertDialog(
                // ... 对话框内容保持不变 ...
                dismissButton = { TextButton(onClick = { showAddRecordDialog = false }) { Text("取消") } }
            )
        }
    }
```

**说明**：用 `key(book.id, showAddRecordDialog)` 包裹对话框内部，确保每次打开对话框或切换书籍时，`inputText` 和 `isIncrement` 状态正确重置。

---

### 文件 4：`presentation/viewmodel/BookDetailViewModel.kt`

#### 修改 4.1 — 重写 `addProgress()` 方法（第 359-451 行）

将整个 `addProgress` 方法替换为以下实现：

**修改前**（原始代码，第 359-431 行）：
```kotlin
    fun addProgress(amount: Double, isIncrement: Boolean = true) {
        val currentBook = _uiState.value.book ?: return
        val isChapterBased = currentBook.progressType == ProgressType.CHAPTER

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()

                val record: ReadingRecordEntity
                val updatedBook: BookEntity

                if (isChapterBased) {
                    val chapters = amount.toInt()
                    val fromChapter = currentBook.currentChapter
                    val maxChapter = currentBook.totalChapters ?: 0
                    val toChapter = if (maxChapter > 0) {
                        if (isIncrement) {
                            (fromChapter + chapters).coerceAtMost(maxChapter)
                        } else {
                            chapters.coerceIn(0, maxChapter)
                        }
                    } else {
                        (fromChapter + chapters).coerceAtLeast(0)
                    }
                    val chaptersActuallyRead = (toChapter - fromChapter).coerceAtLeast(0)

                    record = ReadingRecordEntity(
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = chaptersActuallyRead.toDouble(),
                        fromPage = fromChapter.toDouble(),
                        toPage = toChapter.toDouble(),
                        chaptersRead = chaptersActuallyRead,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentChapter = toChapter,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                } else {
                    val fromPage = currentBook.currentPage
                    val toPage = if (isIncrement) {
                        (fromPage + amount).coerceAtMost(currentBook.totalPages)
                    } else {
                        amount.coerceIn(0.0, currentBook.totalPages)
                    }
                    val pagesActuallyRead = (toPage - fromPage).coerceAtLeast(0.0)

                    record = ReadingRecordEntity(
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = pagesActuallyRead,
                        fromPage = fromPage,
                        toPage = toPage,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentPage = toPage,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                }

                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }
```

**修改后**：
```kotlin
    fun addProgress(amount: Double, isIncrement: Boolean = true) {
        val currentBook = _uiState.value.book ?: return
        val isChapterBased = currentBook.progressType == ProgressType.CHAPTER

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val todayStart = getStartOfDay(currentTime)
                val todayEnd = getEndOfDay(currentTime)

                // 查询今日是否已有 NORMAL 记录，用于 upsert
                val todayRecord = recordRepository.getLatestNormalRecordToday(
                    currentBook.id, todayStart, todayEnd
                )

                val record: ReadingRecordEntity
                val updatedBook: BookEntity

                if (isChapterBased) {
                    val chapters = amount.toInt()
                    val fromChapter = currentBook.currentChapter
                    val maxChapter = currentBook.totalChapters ?: 0
                    val toChapter = if (maxChapter > 0) {
                        if (isIncrement) {
                            (fromChapter + chapters).coerceAtMost(maxChapter)
                        } else {
                            chapters.coerceIn(0, maxChapter)
                        }
                    } else {
                        (fromChapter + chapters).coerceAtLeast(0)
                    }
                    val chaptersActuallyRead = (toChapter - fromChapter).coerceAtLeast(0)

                    // 如果今日已有记录，使用其 id 进行 upsert，否则插入新记录
                    val recordId = todayRecord?.id ?: 0L
                    record = ReadingRecordEntity(
                        id = recordId,
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = chaptersActuallyRead.toDouble(),
                        fromPage = fromChapter.toDouble(),
                        toPage = toChapter.toDouble(),
                        chaptersRead = chaptersActuallyRead,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentChapter = toChapter,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                } else {
                    val fromPage = currentBook.currentPage
                    val maxPage = currentBook.totalPages
                    val toPage = if (maxPage > 0) {
                        if (isIncrement) {
                            (fromPage + amount).coerceAtMost(maxPage)
                        } else {
                            amount.coerceIn(0.0, maxPage)
                        }
                    } else {
                        if (isIncrement) {
                            (fromPage + amount).coerceAtLeast(0.0)
                        } else {
                            amount.coerceAtLeast(0.0)
                        }
                    }
                    val pagesActuallyRead = (toPage - fromPage).coerceAtLeast(0.0)

                    // 如果今日已有记录，使用其 id 进行 upsert，否则插入新记录
                    val recordId = todayRecord?.id ?: 0L
                    record = ReadingRecordEntity(
                        id = recordId,
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = pagesActuallyRead,
                        fromPage = fromPage,
                        toPage = toPage,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentPage = toPage,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                }

                // 使用 upsert 方法，当 record.id > 0 时覆盖今日记录，否则插入新记录
                bookRepository.upsertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }
```

**核心变更**：
- 新增 `todayStart`/`todayEnd` 计算（第 366-367 行）
- 新增 `recordRepository.getLatestNormalRecordToday()` 查询（第 370-372 行）
- 章节和页数分支中均增加 `val recordId = todayRecord?.id ?: 0L`，并将 `id = recordId` 传入 `ReadingRecordEntity`
- 最终调用从 `insertRecordAndUpdateBook` 改为 `upsertRecordAndUpdateBook`（第 446 行）
- 页数分支增加 `maxPage == 0` 时的保护逻辑（第 413-427 行）

#### 修改 4.2 — 新增 `updateProgressType()` 方法（第 521-534 行）

在 `updateRating()` 方法之后、类的闭合大括号 `}` 之前，追加：

```kotlin

    fun updateProgressType(progressType: ProgressType) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val updatedBook = currentBook.copy(
                    progressType = progressType,
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新进度类型失败: ${e.message}") }
            }
        }
    }
```

---

### 文件 5：`data/local/dao/ReadingRecordDao.kt`

#### 修改 5.1 — 新增查询方法（第 40-42 行）

在 `getRecordsByBookIdOnce` 方法之后、接口的闭合大括号 `}` 之前，追加：

```kotlin

    /** 查询指定书籍在指定日期范围内的最新一条 NORMAL 记录（用于同日 upsert） */
    @Query("SELECT * FROM reading_records WHERE bookId = :bookId AND date >= :dayStart AND date < :dayEnd AND recordType = 'NORMAL' ORDER BY date DESC LIMIT 1")
    suspend fun getLatestNormalRecordToday(bookId: Long, dayStart: Long, dayEnd: Long): ReadingRecordEntity?
```

---

### 文件 6：`domain/repository/ReadingRecordRepository.kt`

#### 修改 6.1 — 新增接口方法（第 14-15 行）

在 `deleteRecord` 方法之后、接口的闭合大括号 `}` 之前，追加：

```kotlin
    /** 查询指定书籍在指定日期范围内的最新一条 NORMAL 记录（用于同日 upsert） */
    suspend fun getLatestNormalRecordToday(bookId: Long, dayStart: Long, dayEnd: Long): ReadingRecordEntity?
```

### 文件 6 对应实现：`data/repository/ReadingRecordRepositoryImpl.kt`

#### 修改 6.2 — 实现接口方法（第 43-44 行）

在 `deleteRecord` 方法之后、类的闭合大括号 `}` 之前，追加：

```kotlin

    override suspend fun getLatestNormalRecordToday(bookId: Long, dayStart: Long, dayEnd: Long): ReadingRecordEntity? =
        readingRecordDao.getLatestNormalRecordToday(bookId, dayStart, dayEnd)
```

---

### 文件 7：`domain/repository/BookRepository.kt`

#### 修改 7.1 — 新增接口方法（第 24-28 行）

在 `insertRecordAndUpdateBook` 方法之后插入：

```kotlin

    /**
     * 原子操作：upsert 阅读记录 + 更新书籍进度/时间
     * 当 record.id > 0 时覆盖已有记录（同日 upsert），否则插入新记录
     */
    suspend fun upsertRecordAndUpdateBook(record: ReadingRecordEntity, book: BookEntity)
```

### 文件 7 对应实现：`data/repository/BookRepositoryImpl.kt`

#### 修改 7.2 — 实现接口方法（第 65-73 行）

在 `insertRecordAndUpdateBook` 方法之后插入：

```kotlin

    override suspend fun upsertRecordAndUpdateBook(record: ReadingRecordEntity, book: BookEntity) {
        database.withTransaction {
            // insertRecord 使用 OnConflictStrategy.REPLACE，
            // 当 record.id > 0 时会覆盖已有记录，id = 0 时插入新记录
            readingRecordDao.insertRecord(record)
            bookDao.updateBook(book)
        }
        triggerBadgeCheck()
    }
```

---

## 不应提交的文件

以下文件是本地构建环境的临时修改，**不要**包含在 PR 中：

| 文件 | 原因 |
|------|------|
| `app/build.gradle.kts` | compileSdk/buildToolsVersion 修改仅为本地构建适配 |
| `local.properties` | 本地 SDK 路径和签名密码，属于 gitignore 文件 |
| `gradlew.bat` | 本地生成的 Gradle Wrapper 脚本 |
| `app/readtrack.keystore` | 本地生成的签名密钥，不应入库 |

---

## PR 提交建议

**标题建议**：
> feat: 支持每本书独立切换进度单位 + 同日记录 Upsert + 对话框刷新修复

**描述建议**：
> - 添加书籍页面和书籍详情页新增"页数/章节数"进度类型切换器
> - 同一天多次更新阅读进度时自动覆盖当日记录（upsert），避免记录膨胀
> - 修复更新进度对话框状态残留问题（使用 key 包裹 remember）
> - 新增 ReadingRecordDao.getLatestNormalRecordToday 查询方法
> - 新增 BookRepository.upsertRecordAndUpdateBook 原子操作
> - 新增 BookDetailViewModel.updateProgressType 方法
