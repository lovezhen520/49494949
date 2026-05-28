package com.webide.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webide.app.domain.model.Project
import com.webide.app.domain.model.ProjectFile
import com.webide.app.domain.model.FileType
import com.webide.app.ui.components.CodeEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    files: List<ProjectFile>,
    onBackClick: () -> Unit,
    onSaveFile: (ProjectFile) -> Unit,
    onNewFile: (String, FileType) -> Unit,
    onDeleteFile: (ProjectFile) -> Unit,
    onRunProject: () -> Unit,
    onExportProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFileId by remember { mutableStateOf(files.firstOrNull()?.id ?: 0L) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ProjectFile?>(null) }
    var currentContent by remember { mutableStateOf("") }

    val selectedFile = remember(files, selectedFileId) {
        files.find { it.id == selectedFileId }
    }

    LaunchedEffect(selectedFile) {
        currentContent = selectedFile?.content ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedFile?.let {
                            onSaveFile(it.copy(content = currentContent))
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                    IconButton(onClick = onExportProject) {
                        Icon(Icons.Default.Share, contentDescription = "导出")
                    }
                    IconButton(onClick = onRunProject) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "运行")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FileTree(
                files = files,
                selectedFileId = selectedFileId,
                onFileSelect = { selectedFileId = it.id },
                onNewFile = { showNewFileDialog = true },
                onDeleteFile = { fileToDelete = it },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
            )
            
            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
            
            selectedFile?.let { file ->
                CodeEditor(
                    code = currentContent,
                    onCodeChange = { currentContent = it },
                    fileType = file.type,
                    modifier = Modifier.weight(1f)
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("请选择一个文件")
                }
            }
        }
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name, type ->
                onNewFile(name, type)
                showNewFileDialog = false
            }
        )
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除文件 \"${file.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(file)
                        fileToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun FileTree(
    files: List<ProjectFile>,
    selectedFileId: Long,
    onFileSelect: (ProjectFile) -> Unit,
    onNewFile: () -> Unit,
    onDeleteFile: (ProjectFile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "文件",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onNewFile) {
                Icon(Icons.Default.Add, contentDescription = "添加文件")
            }
        }

        files.forEach { file ->
            FileItem(
                file = file,
                isSelected = file.id == selectedFileId,
                onClick = { onFileSelect(file) },
                onDelete = { onDeleteFile(file) }
            )
        }
    }
}

@Composable
fun FileItem(
    file: ProjectFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = file.name,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, FileType) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(FileType.HTML) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("文件类型:")
                Column {
                    FileType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selectedType == type)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedType = type }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onConfirm(fileName, selectedType)
                    }
                },
                enabled = fileName.isNotBlank()
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
