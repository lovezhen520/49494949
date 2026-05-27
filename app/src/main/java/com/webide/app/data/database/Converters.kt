package com.webide.app.data.database

import androidx.room.TypeConverter
import com.webide.app.domain.model.FileType

class Converters {
    @TypeConverter
    fun fromFileType(fileType: FileType): String {
        return fileType.name
    }

    @TypeConverter
    fun toFileType(name: String): FileType {
        return FileType.valueOf(name)
    }
}
