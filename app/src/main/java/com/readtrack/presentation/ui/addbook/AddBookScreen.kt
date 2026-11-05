package com.readtrack.presentation.ui.addbook

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.readtrack.data.local.entity.BookTypeEntity
import com.readtrack.data.remote.BingImageResult
import com.readtrack.presentation.ui.components.BookCover
import com.readtrack.presentation.ui.components.BookCoverQuality
import com.readtrack.presentation.ui.components.buildBookImageRequest
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.components.getStatusColor
import com.readtrack.presentation.viewmodel.AddBookViewModel
import com.readtrack.domain.model.ProgressType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBookScreen(
    onNavigateBack: () -> Unit,
    bookId: Long?,
    onSearchCover: (String) -> Unit = {},
    onPickCover: () -> Unit = {},
    viewModel: AddBookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.updateCoverUri(uri?.toString())
    }

    // 网络图片搜索弹窗（替代原 URL 输入弹窗）
    // URL输入弹窗状态（保留，作为直接URL输入的备选）
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showCreateTypeDialog by remember { mutableStateOf(false) }
    // 长按类型后的管理弹窗
    var typeToManage by remember { mutableStateOf<BookTypeEntity?>(null) }
    /** 受影响的书籍数；null 表示仍在加载，此时不展示数字以免误导删除决策 */
    var typeAffectedCount by remember { mutableStateOf<Int?>(null) }
    var typeManageError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // 使用 rememberSaveable 追踪是否已经初始化过，避免从封面选择器返回时重置状态
    var hasInitialized by rememberSaveable { mutableStateOf(false) }
    
    // Load book data if editing
    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.loadBook(bookId)
            hasInitialized = true
        } else if (!hasInitialized) {
            // 只有在首次进入添加页面时才重置，避免从封面选择器返回时清空数据
            viewModel.resetState()
            hasInitialized = true
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // URL输入弹窗
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUrlDialog = false
                urlInput = ""
                urlError = null
            },
            title = { 
                Text("网络导入封面", fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    Text(
                        "输入图片的网络链接地址",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { 
                            urlInput = it
                            urlError = null
                        },
                        label = { Text("图片URL") },
                        placeholder = { Text("https://example.com/cover.jpg") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            urlInput.isBlank() -> urlError = "请输入图片地址"
                            !urlInput.startsWith("http://") && !urlInput.startsWith("https://") -> urlError = "请输入以 http:// 或 https:// 开头的地址"
                            else -> {
                                viewModel.updateCoverUri(urlInput.trim())
                                showUrlDialog = false
                                urlInput = ""
                                urlError = null
                            }
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUrlDialog = false
                    urlInput = ""
                    urlError = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditing) "编辑书籍" else "添加书籍", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveBook() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "保存",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover Image Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 封面预览
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.coverUri != null) {
                            BookCover(
                                coverPath = uiState.coverUri,
                                contentDescription = "书籍封面",
                                modifier = Modifier.fillMaxSize(),
                                requestSize = DpSize(240.dp, 320.dp),
                                quality = BookCoverQuality.PREVIEW
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "📖",
                                    fontSize = 48.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("本地导入")
                        }
                        OutlinedButton(
                            onClick = { viewModel.showImageSearchDialog(uiState.title) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("网络搜索")
                        }
                    }
                    // 小提示：也支持直接输入URL
                    TextButton(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "或直接输入图片URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Book Title with Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("书名 *") },
                    placeholder = { Text("请输入书名") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilledTonalIconButton(
                    onClick = { viewModel.showSearchDialog() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "搜索书籍")
                }
            }
            
            // 搜索提示文字
            Text(
                text = "💡 点击搜索图标可从网络自动填充书籍信息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Author
            OutlinedTextField(
                value = uiState.author,
                onValueChange = { viewModel.updateAuthor(it) },
                label = { Text("作者") },
                placeholder = { Text("请输入作者（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            // Publisher
            OutlinedTextField(
                value = uiState.publisher,
                onValueChange = { viewModel.updatePublisher(it) },
                label = { Text("出版社") },
                placeholder = { Text("请输入出版社（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

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

            // Progress Fields (auto-determined by settings)
            if (uiState.progressType == ProgressType.PAGE) {
                // Page-based progress
                OutlinedTextField(
                    value = uiState.totalPages,
                    onValueChange = { viewModel.updateTotalPages(it) },
                    label = { Text("总页数 *") },
                    placeholder = { Text("请输入总页数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

            } else {
                // Chapter-based progress
                OutlinedTextField(
                    value = uiState.totalChapters,
                    onValueChange = { viewModel.updateTotalChapters(it) },
                    label = { Text("总章节数 *") },
                    placeholder = { Text("请输入总章节数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Book Type Selection
            Text(
                "书籍类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.allBookTypes.forEach { type ->
                    val isSelected = uiState.bookType == type.name
                    BookTypeChip(
                        text = type.name,
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        labelColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = { viewModel.updateBookType(type.name) },
                        onLongClick = {
                            typeToManage = type
                            typeManageError = null
                            typeAffectedCount = null
                            scope.launch {
                                typeAffectedCount = viewModel.countBooksWithType(type.name)
                            }
                        }
                    )
                }
                // 新建类型按钮：复用 BookTypeChip，保证与类型项高度、内边距完全一致
                BookTypeChip(
                    text = "+ 新建类型",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.primary,
                    onClick = { showCreateTypeDialog = true }
                )
            }

            Text(
                "长按类型可重命名或删除，改名后已使用该类型的书籍会同步更新",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tag Selection
            Text(
                "标签（可选）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.allTags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in uiState.selectedTagIds,
                        onClick = { viewModel.toggleTag(tag.id) },
                        label = { Text(tag.name, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                // 新建标签按钮
                FilterChip(
                    selected = false,
                    onClick = { showCreateTagDialog = true },
                    label = { Text("+ 新建标签", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Book Status Selection
            Text(
                "书籍状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 5个状态使用2行布局：第1行3个，第2行2个
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookStatus.entries.take(3).forEach { status ->
                        FilterChip(
                            selected = uiState.status == status,
                            onClick = { viewModel.updateStatus(status) },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        status.displayName,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getStatusColor(status),
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookStatus.entries.drop(3).forEach { status ->
                        FilterChip(
                            selected = uiState.status == status,
                            onClick = { viewModel.updateStatus(status) },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        status.displayName,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getStatusColor(status),
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    // 填充空白保持对齐
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("简介") },
                placeholder = { Text("请输入书籍简介（可选）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // 书籍搜索 Bottom Sheet
    if (uiState.showSearchDialog) {
        BookSearchBottomSheet(
            isSearching = uiState.isSearching,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            searchError = uiState.searchError,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onDismiss = { viewModel.hideSearchDialog() },
            onSelectBook = { viewModel.fillFromSearchResult(it) }
        )
    }

    // 网络图片搜索 Bottom Sheet
    if (uiState.showImageSearchDialog) {
        ImageSearchBottomSheet(
            searchQuery = uiState.imageSearchQuery,
            imageResults = uiState.imageSearchResults,
            isSearching = uiState.isImageSearching,
            isLoadingMore = uiState.isLoadingMoreImages,
            hasMore = uiState.hasMoreImages,
            searchError = uiState.imageSearchError,
            selectedImageUrl = uiState.selectedImageUrl,
            onQueryChange = { viewModel.updateImageSearchQuery(it) },
            onDismiss = { viewModel.hideImageSearchDialog() },
            onSelectImage = { viewModel.selectImage(it) },
            onPreviewImage = { viewModel.previewImage(it) },
            onClearPreview = { viewModel.clearPreview() },
            onLoadMore = { viewModel.loadMoreImages() }
        )
    }

    // 创建标签弹窗
    if (showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { tagName ->
                scope.launch {
                    viewModel.createTagAndSelect(tagName)
                    showCreateTagDialog = false
                }
            }
        )
    }

    // 创建书籍类型弹窗
    if (showCreateTypeDialog) {
        CreateBookTypeDialog(
            existingNames = uiState.allBookTypes.map { it.name },
            onDismiss = { showCreateTypeDialog = false },
            onConfirm = { typeName ->
                scope.launch {
                    viewModel.createBookTypeAndSelect(typeName)
                    showCreateTypeDialog = false
                }
            }
        )
    }

    // 长按类型后的管理弹窗（重命名 / 删除）
    typeToManage?.let { type ->
        BookTypeManageDialog(
            type = type,
            affectedCount = typeAffectedCount,
            isLastType = uiState.allBookTypes.size <= 1,
            existingNames = uiState.allBookTypes.map { it.name },
            errorMessage = typeManageError,
            onDismiss = {
                typeToManage = null
                typeManageError = null
            },
            onRename = { newName ->
                scope.launch {
                    val ok = viewModel.renameBookType(type.id, newName)
                    if (ok) {
                        typeToManage = null
                        typeManageError = null
                    } else {
                        typeManageError = "重命名失败：类型名不能为空，或已存在同名类型"
                    }
                }
            },
            onDelete = {
                scope.launch {
                    val ok = viewModel.deleteBookType(type.id)
                    if (ok) {
                        typeToManage = null
                        typeManageError = null
                    } else {
                        typeManageError = "删除失败：至少需要保留一个书籍类型"
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookSearchBottomSheet(
    isSearching: Boolean,
    searchQuery: String,
    searchResults: List<com.readtrack.data.remote.BookSearchResult>,
    searchError: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectBook: (com.readtrack.data.remote.BookSearchResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = "搜索书籍",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                label = { Text("输入书名或作者") },
                placeholder = { Text("例如：活着 三体") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
            
            // 数据来源提示
            Text(
                text = "数据来源：豆瓣搜索",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            
            // 错误提示
            searchError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // 搜索结果列表
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "找到 ${searchResults.size} 本书",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { "${it.title}-${it.author}-${it.coverUrl}" }
                    ) { book ->
                        BookSearchResultItem(
                            book = book,
                            onClick = { onSelectBook(book) }
                        )
                        HorizontalDivider()
                    }
                }
            } else if (!isSearching && searchQuery.length >= 2 && searchError == null) {
                Text(
                    text = "未找到相关书籍，试试其他关键词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (searchQuery.isEmpty()) {
                Text(
                    text = "输入书名或作者名开始搜索",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun BookSearchResultItem(
    book: com.readtrack.data.remote.BookSearchResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverModel = remember(book.coverUrl) {
        book.coverUrl?.let {
            buildBookImageRequest(
                context = context,
                imageUrl = it,
                requestedSizePx = 120 to 160,
                quality = BookCoverQuality.THUMBNAIL
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面缩略图
        AsyncImage(
            model = coverModel,
            contentDescription = book.title,
            modifier = Modifier
                .size(60.dp, 80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            onError = {
                // 图片加载失败时显示占位符
            }
        )
        
        // 书籍信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            
            book.author?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                book.publishYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                book.publisher?.let { publisher ->
                    Text(
                        text = publisher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
        
        // 选中提示
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .graphicsLayer { this.rotationZ = 180f }
                .size(20.dp)
        )
    }
}

// ─── Bing 图片搜索 Bottom Sheet ───

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ImageSearchBottomSheet(
    searchQuery: String,
    imageResults: List<BingImageResult>,
    isSearching: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    searchError: String?,
    selectedImageUrl: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectImage: (BingImageResult) -> Unit,
    onPreviewImage: (String) -> Unit,
    onClearPreview: () -> Unit,
    onLoadMore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 图片预览 Dialog
    if (selectedImageUrl != null) {
        AlertDialog(
            onDismissRequest = onClearPreview,
            confirmButton = {
                TextButton(onClick = onClearPreview) {
                    Text("关闭预览")
                }
            },
            text = {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "封面预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = "搜索封面图片",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 搜索输入框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                label = { Text("输入书名搜索封面") },
                placeholder = { Text("例如：活着 三体") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清空")
                        }
                    }
                }
            )

            // 数据来源提示
            Text(
                text = "图片来源：Bing 图片搜索",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // 错误提示
            searchError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 搜索结果网格
            if (imageResults.isNotEmpty()) {
                Text(
                    text = "找到 ${imageResults.size} 张图片，点击选为封面，长按预览",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 使用 Grid 方式展示（3列）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 将结果按 3 列排列
                        val chunkedResults = imageResults.chunked(3)
                        items(chunkedResults.size, key = { it }) { rowIndex ->
                            val row = chunkedResults[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { image ->
                                    val loadUrl = image.getLoadUrl()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .combinedClickable(
                                                onClick = { onSelectImage(image) },
                                                onLongClick = { onPreviewImage(image.fullUrl.ifBlank { loadUrl }) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = loadUrl,
                                            contentDescription = image.title,
                                            modifier = Modifier.fillMaxSize(),
                                            onError = {
                                                // 图片加载失败时不做处理
                                            }
                                        )
                                        // 长按提示
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "长按预览",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                // 补齐空白（如果该行不足3个）
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // 加载更多按钮
                        if (hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                "加载更多图片...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        TextButton(onClick = onLoadMore) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("加载更多图片")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!isSearching && searchQuery.length >= 2 && searchError == null) {
                Text(
                    text = "未找到相关图片，试试其他关键词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (searchQuery.isEmpty()) {
                Text(
                    text = "输入书名开始搜索封面图片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新标签", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("标签名称") },
                placeholder = { Text("输入标签名称") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tagName.trim()) },
                enabled = tagName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateBookTypeDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var typeName by remember { mutableStateOf("") }
    val trimmed = typeName.trim()
    val isDuplicate = trimmed.isNotEmpty() && existingNames.any { it == trimmed }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建书籍类型", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = typeName,
                    onValueChange = { typeName = it },
                    label = { Text("类型名称") },
                    placeholder = { Text("如：技术书、绘本、杂志") },
                    singleLine = true,
                    isError = isDuplicate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isDuplicate) {
                    Text(
                        "该类型已存在",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && !isDuplicate
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 书籍类型块。「新建类型」按钮也复用本组件，
 * 保证与类型项共享同一套高度、内边距与字体规格，视觉上严格齐平。
 *
 * 用 Surface + 手势宿主而非 FilterChip 外裹手势：
 * FilterChip 自带点击处理，叠加长按检测器后松手可能同时触发选中，
 * 出现「弹框的同时误改了书籍类型」的事件冲突。单一手势宿主可避免。
 *
 * @param onLongClick 为 null 时只响应点击（如「新建类型」按钮）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookTypeChip(
    text: String,
    containerColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val gestureModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = gestureModifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

private enum class BookTypeManageMode { MANAGE, RENAME, CONFIRM_DELETE }

/**
 * 长按书籍类型后的管理框：重命名或删除。
 *
 * 重命名会同步所有已使用该类型的书籍（由仓库层在同一事务内完成 reassignBooks）；
 * 删除会把关联书籍归入剩余类型，因此作为破坏性操作需要二次确认。
 *
 * @param affectedCount 受影响的书籍数；null 表示仍在统计，此时不展示数字以免误导删除决策
 */
@Composable
private fun BookTypeManageDialog(
    type: BookTypeEntity,
    affectedCount: Int?,
    isLastType: Boolean,
    existingNames: List<String>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var mode by remember(type.id) { mutableStateOf(BookTypeManageMode.MANAGE) }
    var newName by remember(type.id) { mutableStateOf(type.name) }

    val trimmed = newName.trim()
    val isDuplicate = trimmed.isNotEmpty() && trimmed != type.name &&
        existingNames.any { it == trimmed }

    val affectedText = when {
        affectedCount == null -> "正在统计使用该类型的书籍…"
        affectedCount == 0 -> "当前没有书籍使用该类型。"
        else -> "有 $affectedCount 本书正在使用该类型。"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理书籍类型", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "「${type.name}」",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    affectedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (mode) {
                    BookTypeManageMode.RENAME -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("新的类型名称") },
                            singleLine = true,
                            isError = isDuplicate,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isDuplicate) {
                            Text(
                                "该类型已存在",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        } else {
                            Text(
                                "改名后，已使用该类型的书籍会同步更新",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    BookTypeManageMode.CONFIRM_DELETE -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            when {
                                isLastType -> "至少需要保留一个书籍类型，无法删除。"
                                (affectedCount ?: 0) > 0 ->
                                    "删除后，这 $affectedCount 本书将自动归入剩余的第一个类型。"
                                else -> "确定要删除该类型吗？"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    BookTypeManageMode.MANAGE -> Unit
                }

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            when (mode) {
                BookTypeManageMode.RENAME -> {
                    TextButton(
                        onClick = { onRename(trimmed) },
                        enabled = trimmed.isNotEmpty() && trimmed != type.name && !isDuplicate
                    ) { Text("保存") }
                }
                BookTypeManageMode.CONFIRM_DELETE -> {
                    TextButton(onClick = onDelete, enabled = !isLastType) {
                        Text("确认删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                BookTypeManageMode.MANAGE -> {
                    TextButton(onClick = { mode = BookTypeManageMode.CONFIRM_DELETE }) {
                        Text("删除类型", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            when (mode) {
                BookTypeManageMode.MANAGE -> {
                    TextButton(onClick = { mode = BookTypeManageMode.RENAME }) {
                        Text("重命名")
                    }
                }
                BookTypeManageMode.RENAME -> {
                    TextButton(onClick = { mode = BookTypeManageMode.MANAGE }) { Text("返回") }
                }
                BookTypeManageMode.CONFIRM_DELETE -> {
                    TextButton(onClick = { mode = BookTypeManageMode.MANAGE }) { Text("取消") }
                }
            }
        }
    )
}
