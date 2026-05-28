package com.webide.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webide.app.data.database.AppDatabase
import com.webide.app.domain.model.Project
import com.webide.app.domain.model.ProjectFile
import com.webide.app.domain.model.FileType
import com.webide.app.domain.repository.ProjectRepository
import com.webide.app.ui.screens.EditorScreen
import com.webide.app.ui.screens.HomeScreen
import com.webide.app.ui.screens.PreviewScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class ScreenType {
    HOME, EDITOR, PREVIEW, SETTINGS
}

class MainViewModel(
    private val repository: ProjectRepository,
    private val preferences: SharedPreferences
) : ViewModel() {
    private val _currentScreen = MutableStateFlow<ScreenType>(ScreenType.HOME)
    val currentScreen: StateFlow<ScreenType> = _currentScreen.asStateFlow()

    private val _themeMode = MutableStateFlow<ThemeMode>(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long>(0)
    val selectedProjectId: StateFlow<Long> = _selectedProjectId.asStateFlow()

    val projects = repository.getAllProjects()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _currentFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val currentFiles: StateFlow<List<ProjectFile>> = _currentFiles.asStateFlow()

    private fun getSavedThemeMode(): ThemeMode {
        val mode = preferences.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        return ThemeMode.valueOf(mode)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun navigateTo(screen: ScreenType, projectId: Long = 0) {
        _currentScreen.value = screen
        if (projectId > 0) {
            _selectedProjectId.value = projectId
            loadProject(projectId)
        }
    }

    private fun loadProject(projectId: Long) {
        viewModelScope.launch {
            _currentProject.value = repository.getProjectById(projectId)
            repository.getFilesByProjectId(projectId).collect { files ->
                _currentFiles.value = files
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val projectId = repository.createProject(name)
            navigateTo(ScreenType.EDITOR, projectId)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun saveFile(file: ProjectFile) {
        viewModelScope.launch {
            repository.saveFile(file)
            _currentProject.value?.let {
                repository.updateProject(it.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun createFile(name: String, type: FileType) {
        viewModelScope.launch {
            _currentProject.value?.let {
                val file = ProjectFile(
                    projectId = it.id,
                    name = name,
                    path = name,
                    type = type,
                    content = ""
                )
                repository.saveFile(file)
            }
        }
    }

    fun deleteFile(file: ProjectFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
        }
    }

    suspend fun exportProject(_project: Project, files: List<ProjectFile>): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val zipFile = File(downloadsDir, "${_project.name}.zip")
        return try {
            repository.exportProjectToZip(_project, files, zipFile)
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { ProjectRepository(this) }
    private val preferences by lazy { getSharedPreferences("webide_prefs", MODE_PRIVATE) }
    private val viewModel by viewModels<MainViewModel> {
        MainViewModelFactory(repository, preferences)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            exportCurrentProject()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            MainApp(viewModel = viewModel)
        }
    }

    fun checkPermissionsAndExport() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (permissions.isEmpty()) {
            exportCurrentProject()
        } else {
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isEmpty()) {
                exportCurrentProject()
            } else {
                requestPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    private fun exportCurrentProject() {
        viewModel.viewModelScope.launch {
            viewModel.currentProject.value?.let { project ->
                val zipFile = viewModel.exportProject(project, viewModel.currentFiles.value)
                zipFile?.let {
                    shareFile(it)
                }
            }
        }
    }

    private fun shareFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(intent, "导出项目"))
    }
}

class MainViewModelFactory(
    private val repository: ProjectRepository,
    private val preferences: SharedPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    
    val darkMode = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.ui.platform.LocalContext.current.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
        typography = MaterialTheme.typography
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = { BottomNavBar(viewModel) }
        ) { paddingValues ->
            val currentScreen by viewModel.currentScreen.collectAsState()
            val projects by viewModel.projects.collectAsState(initial = emptyList())
            val currentProject by viewModel.currentProject.collectAsState()
            val currentFiles by viewModel.currentFiles.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? MainActivity

            when (currentScreen) {
                ScreenType.HOME -> {
                    HomeScreen(
                        projects = projects,
                        onProjectClick = { project ->
                            viewModel.navigateTo(ScreenType.EDITOR, project.id)
                        },
                        onNewProject = { name ->
                            viewModel.createProject(name)
                        },
                        onDeleteProject = { project ->
                            viewModel.deleteProject(project)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                ScreenType.EDITOR -> {
                    currentProject?.let { project ->
                        EditorScreen(
                            project = project,
                            files = currentFiles,
                            onBackClick = {
                                viewModel.navigateTo(ScreenType.HOME)
                            },
                            onSaveFile = { file ->
                                viewModel.saveFile(file)
                            },
                            onNewFile = { name, type ->
                                viewModel.createFile(name, type)
                            },
                            onDeleteFile = { file ->
                                viewModel.deleteFile(file)
                            },
                            onRunProject = {
                                viewModel.navigateTo(ScreenType.PREVIEW, project.id)
                            },
                            onExportProject = {
                                activity?.checkPermissionsAndExport()
                            },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
                ScreenType.PREVIEW -> {
                    val projectId = viewModel.selectedProjectId.value
                    PreviewScreen(
                        files = currentFiles,
                        onBackClick = {
                            viewModel.navigateTo(ScreenType.EDITOR, projectId)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                ScreenType.SETTINGS -> {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            viewModel.setThemeMode(mode)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.FolderOpen, contentDescription = "项目") },
            label = { Text("项目") },
            selected = currentScreen == ScreenType.HOME,
            onClick = { viewModel.navigateTo(ScreenType.HOME) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Code, contentDescription = "编辑器") },
            label = { Text("编辑") },
            selected = currentScreen == ScreenType.EDITOR,
            onClick = { 
                if (viewModel.selectedProjectId.value > 0) {
                    viewModel.navigateTo(ScreenType.EDITOR, viewModel.selectedProjectId.value)
                } else {
                    viewModel.navigateTo(ScreenType.HOME)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "预览") },
            label = { Text("预览") },
            selected = currentScreen == ScreenType.PREVIEW,
            onClick = { 
                if (viewModel.selectedProjectId.value > 0) {
                    viewModel.navigateTo(ScreenType.PREVIEW, viewModel.selectedProjectId.value)
                } else {
                    viewModel.navigateTo(ScreenType.HOME)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
            label = { Text("设置") },
            selected = currentScreen == ScreenType.SETTINGS,
            onClick = { viewModel.navigateTo(ScreenType.SETTINGS) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "主题设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeChange(mode) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "亮色模式"
                                    ThemeMode.DARK -> "暗色模式"
                                    ThemeMode.SYSTEM -> "跟随系统"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WebIDE",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "一款强大的移动端 Web 开发工具，支持 HTML、CSS、JavaScript 编辑和实时预览。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun lightColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF06B6D4),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFFAFE),
        onSecondaryContainer = Color(0xFF083344),
        tertiary = Color(0xFF14B8A6),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCCFBF0),
        onTertiaryContainer = Color(0xFF134E4A),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF171717),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF171717),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF525252),
        error = Color(0xFFDC2626),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
        outline = Color(0xFFA3A3A3),
        outlineVariant = Color(0xFFD4D4D4),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF171717),
        inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF93C5FD),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE5E5E5)
    )
}

fun darkColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF1E3A8A),
        primaryContainer = Color(0xFF1E40AF),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF22D3EE),
        onSecondary = Color(0xFF083344),
        secondaryContainer = Color(0xFF0C4A5E),
        onSecondaryContainer = Color(0xFFCFFAFE),
        tertiary = Color(0xFF2DD4BF),
        onTertiary = Color(0xFF134E4A),
        tertiaryContainer = Color(0xFF1A4D49),
        onTertiaryContainer = Color(0xFFCCFBF0),
        background = Color(0xFF0F172A),
        onBackground = Color(0xFFE2E8F0),
        surface = Color(0xFF1E293B),
        onSurface = Color(0xFFE2E8F0),
        surfaceVariant = Color(0xFF334155),
        onSurfaceVariant = Color(0xFFCBD5E1),
        error = Color(0xFFFCA5A5),
        onError = Color(0xFF7F1D1D),
        errorContainer = Color(0xFF991B1B),
        onErrorContainer = Color(0xFFFECACA),
        outline = Color(0xFF64748B),
        outlineVariant = Color(0xFF334155),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE2E8F0),
        inverseOnSurface = Color(0xFF1E293B),
        inversePrimary = Color(0xFF2563EB),
        surfaceBright = Color(0xFF334155),
        surfaceDim = Color(0xFF1E293B)
    )
}
