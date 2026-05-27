package com.webide.app.domain.repository

import android.content.Context
import com.webide.app.data.database.AppDatabase
import com.webide.app.domain.model.Project
import com.webide.app.domain.model.ProjectFile
import com.webide.app.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val projectDao = database.projectDao()
    private val fileDao = database.fileDao()

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(projectId: Long): Project? = projectDao.getProjectById(projectId)

    fun getFilesByProjectId(projectId: Long): Flow<List<ProjectFile>> = 
        fileDao.getFilesByProjectId(projectId)

    suspend fun getFileById(fileId: Long): ProjectFile? = fileDao.getFileById(fileId)

    suspend fun createProject(name: String): Long = withContext(Dispatchers.IO) {
        val projectDir = File(context.filesDir, "projects/$name")
        projectDir.mkdirs()
        
        val project = Project(
            name = name,
            path = projectDir.absolutePath
        )
        val projectId = projectDao.insertProject(project)

        val indexHtml = ProjectFile(
            projectId = projectId,
            name = "index.html",
            path = "index.html",
            type = FileType.HTML,
            content = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$name</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <h1>Hello, WebIDE!</h1>
    <p>欢迎使用 WebIDE</p>
    <script src="script.js"></script>
</body>
</html>"""
        )
        fileDao.insertFile(indexHtml)

        val styleCss = ProjectFile(
            projectId = projectId,
            name = "style.css",
            path = "style.css",
            type = FileType.CSS,
            content = """body {
    font-family: Arial, sans-serif;
    margin: 20px;
    background-color: #f0f4f8;
}

h1 {
    color: #1a73e8;
}"""
        )
        fileDao.insertFile(styleCss)

        val scriptJs = ProjectFile(
            projectId = projectId,
            name = "script.js",
            path = "script.js",
            type = FileType.JS,
            content = """console.log('WebIDE 已加载');

document.addEventListener('DOMContentLoaded', function() {
    console.log('页面已准备好');
});"""
        )
        fileDao.insertFile(scriptJs)

        projectId
    }

    suspend fun updateProject(project: Project) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(project: Project) = withContext(Dispatchers.IO) {
        val projectDir = File(project.path)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        projectDao.deleteProject(project)
    }

    suspend fun saveFile(file: ProjectFile) = withContext(Dispatchers.IO) {
        if (file.id == 0L) {
            fileDao.insertFile(file)
        } else {
            fileDao.updateFile(file)
        }
    }

    suspend fun deleteFile(file: ProjectFile) = withContext(Dispatchers.IO) {
        fileDao.deleteFile(file)
    }

    suspend fun exportProjectToZip(project: Project, files: List<ProjectFile>, zipFile: File) = 
        withContext(Dispatchers.IO) {
            ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                files.forEach { file ->
                    val entry = ZipEntry(file.name)
                    zipOut.putNextEntry(entry)
                    zipOut.write(file.content.toByteArray())
                    zipOut.closeEntry()
                }
            }
        }

    fun getProjectDir(project: Project): File = File(project.path)
}
