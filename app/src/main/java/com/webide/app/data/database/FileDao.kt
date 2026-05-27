package com.webide.app.data.database

import androidx.room.*
import com.webide.app.domain.model.ProjectFile
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId ORDER BY name")
    fun getFilesByProjectId(projectId: Long): Flow<List<ProjectFile>>

    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileById(fileId: Long): ProjectFile?

    @Insert
    suspend fun insertFile(file: ProjectFile): Long

    @Update
    suspend fun updateFile(file: ProjectFile)

    @Delete
    suspend fun deleteFile(file: ProjectFile)

    @Query("DELETE FROM files WHERE projectId = :projectId")
    suspend fun deleteFilesByProjectId(projectId: Long)
}
