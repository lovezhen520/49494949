package com.webide.app

import android.Manifest
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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

sealed class Screen {
    object Home : Screen()
    data class Editor(val projectId: Long) : Screen()
    data class Preview(val projectId: Long) : Screen()
}

class MainViewModel(
    private val repository: ProjectRepository
) : ViewModel() {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val projects = repository.getAllProjects()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    private val _currentFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val currentFiles: StateFlow<List<ProjectFile>> = _currentFiles.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        when (screen) {
            is Screen.Editor -> loadProject(screen.projectId)
            is Screen.Preview -> loadProject(screen.projectId)
            Screen.Home -> { }
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
            navigateTo(Screen.Editor(projectId))
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
            _currentProject.value?.let { project ->
                repository.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun createFile(name: String, type: FileType) {
        viewModelScope.launch {
            _currentProject.value?.let { project ->
                val file = ProjectFile(
                    projectId = project.id,
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

    suspend fun exportProject(project: Project, files: List<ProjectFile>): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val zipFile = File(downloadsDir, "${project.name}.zip")
        return try {
            repository.exportProjectToZip(project, files, zipFile)
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
    private val viewModel by viewModels<MainViewModel> {
        MainViewModelFactory(repository)
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
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel = viewModel)
                }
            }
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
    private val repository: ProjectRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val currentProject by viewModel.currentProject.collectAsState()
    val currentFiles by viewModel.currentFiles.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? MainActivity

    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                projects = projects,
                onProjectClick = { project ->
                    viewModel.navigateTo(Screen.Editor(project.id))
                },
                onNewProject = { name ->
                    viewModel.createProject(name)
                },
                onDeleteProject = { project ->
                    viewModel.deleteProject(project)
                }
            )
        }
        is Screen.Editor -> {
            val editorScreen = currentScreen as Screen.Editor
            currentProject?.let { project ->
                EditorScreen(
                    project = project,
                    files = currentFiles,
                    onBackClick = {
                        viewModel.navigateTo(Screen.Home)
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
                        viewModel.navigateTo(Screen.Preview(project.id))
                    },
                    onExportProject = {
                        activity?.checkPermissionsAndExport()
                    }
                )
            }
        }
        is Screen.Preview -> {
            val previewScreen = currentScreen as Screen.Preview
            PreviewScreen(
                files = currentFiles,
                onBackClick = {
                    viewModel.navigateTo(Screen.Editor(previewScreen.projectId))
                }
            )
        }
    }
}
