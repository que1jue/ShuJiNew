package com.readtrack.presentation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.readtrack.data.local.entity.BookTypeEntity
import com.readtrack.presentation.viewmodel.BookTypeManagementViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookTypeManagementSheet(
    onDismiss: () -> Unit,
    viewModel: BookTypeManagementViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val types by viewModel.bookTypes.collectAsState()
    val scope = rememberCoroutineScope()

    var newTypeName by remember { mutableStateOf("") }
    var typeToRename by remember { mutableStateOf<BookTypeEntity?>(null) }
    var typeToDelete by remember { mutableStateOf<BookTypeEntity?>(null) }
    var deleteAffectedCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Text(
                "书籍类型管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 创建新类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTypeName,
                    onValueChange = { newTypeName = it },
                    placeholder = { Text("新类型名称") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                FilledTonalButton(
                    onClick = {
                        val name = newTypeName.trim()
                        if (name.isEmpty()) return@FilledTonalButton
                        scope.launch {
                            val created = viewModel.createBookType(name)
                            if (created > 0L) {
                                newTypeName = ""
                                errorMessage = null
                            } else {
                                errorMessage = "类型「$name」已存在"
                            }
                        }
                    },
                    enabled = newTypeName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("创建")
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "提示：删除类型后，该类型下的书籍会自动归入剩余的第一个类型；至少要保留一个类型。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 类型列表
            if (types.isEmpty()) {
                Text(
                    "暂无类型，请先创建一个",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(types, key = { it.id }) { type ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    type.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { typeToRename = type }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "重命名类型",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            deleteAffectedCount = viewModel.countBooksWithType(type.name)
                                            typeToDelete = type
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除类型",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 重命名对话框
    typeToRename?.let { type ->
        RenameBookTypeDialog(
            initialName = type.name,
            existingNames = types.map { it.name },
            onDismiss = { typeToRename = null },
            onConfirm = { newName ->
                scope.launch {
                    val ok = viewModel.renameBookType(type.id, newName)
                    errorMessage = if (ok) null else "重命名失败，类型「$newName」可能已存在"
                    typeToRename = null
                }
            }
        )
    }

    // 删除确认对话框
    typeToDelete?.let { type ->
        val isLastType = types.size <= 1
        AlertDialog(
            onDismissRequest = { typeToDelete = null },
            title = { Text("删除类型", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when {
                        isLastType -> "至少需要保留一个书籍类型，无法删除。"
                        deleteAffectedCount > 0 ->
                            "确定要删除类型「${type.name}」吗？该类型下的 $deleteAffectedCount 本书将自动归入剩余的第一个类型。"
                        else -> "确定要删除类型「${type.name}」吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = viewModel.deleteBookType(type.id)
                            errorMessage = if (ok) null else "删除失败，请至少保留一个类型"
                            typeToDelete = null
                        }
                    },
                    enabled = !isLastType
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { typeToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RenameBookTypeDialog(
    initialName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val isDuplicate = trimmed.isNotEmpty() &&
        trimmed != initialName &&
        existingNames.any { it == trimmed }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名类型", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("类型名称") },
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
                enabled = trimmed.isNotEmpty() && trimmed != initialName && !isDuplicate
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
