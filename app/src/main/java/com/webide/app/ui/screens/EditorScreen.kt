package com.webide.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.webide.app.R
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
    var isFolderExpanded by remember { mutableStateOf(true) }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedFile?.let {
                            onSaveFile(it.copy(content = currentContent))
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
                    IconButton(onClick = onExportProject) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export))
                    }
                    IconButton(onClick = onRunProject) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.run))
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
            AnimatedVisibility(
                visible = isFolderExpanded,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                FileTree(
                    files = files,
                    selectedFileId = selectedFileId,
                    onFileSelect = { selectedFileId = it.id },
                    onNewFile = { showNewFileDialog = true },
                    onDeleteFile = { fileToDelete = it },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(240.dp)
                )
            }

            if (isFolderExpanded) {
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    selectedFile?.let { file ->
                        CodeEditor(
                            code = currentContent,
                            onCodeChange = { currentContent = it },
                            fileType = file.type,
                            fileName = file.name,
                            modifier = Modifier.weight(1f)
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.select_file),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.select_file_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isFolderExpanded) {
                    IconButton(
                        onClick = { isFolderExpanded = false },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .size(36.dp),
                        content = {
                            Icon(
                                Icons.Default.ExpandLess,
                                contentDescription = stringResource(R.string.collapse),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                } else {
                    IconButton(
                        onClick = { isFolderExpanded = true },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .size(36.dp),
                        content = {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = stringResource(R.string.expand),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
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
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text("${stringResource(R.string.confirm_delete_file)} \"${file.name}\" ${stringResource(R.string.confirm_delete_file_end)}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(file)
                        if (selectedFileId == file.id) {
                            selectedFileId = files.firstOrNull { it.id != file.id }?.id ?: 0L
                        }
                        fileToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.cancel))
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.files),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onNewFile,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .size(32.dp),
                content = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_file),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        files.groupBy { getFileExtension(it.name) }.forEach { (extension, fileList) ->
            FolderGroup(
                title = getExtensionLabel(extension),
                files = fileList,
                selectedFileId = selectedFileId,
                onFileSelect = onFileSelect,
                onDeleteFile = onDeleteFile
            )
        }

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.no_files),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FolderGroup(
    title: String,
    files: List<ProjectFile>,
    selectedFileId: Long,
    onFileSelect: (ProjectFile) -> Unit,
    onDeleteFile: (ProjectFile) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    files.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(start = 28.dp)
            ) {
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                getFileIcon(file.type),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = getFileIconColor(file.type)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = file.name,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp),
            content = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
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
        title = { Text(stringResource(R.string.new_file)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(stringResource(R.string.file_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (fileName.isNotBlank() && !fileName.contains(".")) {
                            Text("${stringResource(R.string.suggested_extension)}.${selectedType.name.lowercase()}")
                        }
                    }
                )

                Text(stringResource(R.string.file_type))
                Column {
                    FileType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selectedType == type)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    getFileIcon(type),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = getFileIconColor(type)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(type.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var finalName = fileName
                    if (finalName.isNotBlank() && !fileName.contains(".")) {
                        finalName = "$fileName.${selectedType.name.lowercase()}"
                    }
                    if (finalName.isNotBlank()) {
                        onConfirm(finalName, selectedType)
                    }
                },
                enabled = fileName.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun getFileExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex > 0) fileName.substring(dotIndex + 1).uppercase() else "OTHER"
}

private fun getExtensionLabel(extension: String): String {
    return when (extension) {
        "HTML" -> "HTML"
        "CSS" -> "CSS"
        "JS" -> "JavaScript"
        "JSON" -> "JSON"
        else -> "其他"
    }
}

@Composable
private fun getFileIcon(type: FileType) = when (type) {
    FileType.HTML -> Icons.Default.Html
    FileType.CSS -> Icons.Default.Palette
    FileType.JS -> Icons.Default.Braces
    FileType.JSON -> Icons.Default.Settings
    FileType.OTHER -> Icons.Default.File
}

@Composable
private fun getFileIconColor(type: FileType): Color {
    return when (type) {
        FileType.HTML -> Color(0xFFE34F26)
        FileType.CSS -> Color(0xFF1572B6)
        FileType.JS -> Color(0xFFF7DF1E)
        FileType.JSON -> Color(0xFF2CB67D)
        FileType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
