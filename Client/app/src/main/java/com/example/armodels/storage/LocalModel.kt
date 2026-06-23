package com.example.armodels.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_models")
data class LocalModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val localFilePath: String,
    val serverId: Int? = null,
    val generationMode: String,
    val createdAt: String
)