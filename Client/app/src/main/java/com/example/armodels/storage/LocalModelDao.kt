package com.example.armodels.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: LocalModel): Long

    @Query("SELECT * FROM local_models ORDER BY id DESC")
    fun observeAll(): Flow<List<LocalModel>>

    @Query("SELECT * FROM local_models WHERE id = :id")
    suspend fun getById(id: Int): LocalModel?

    @Query("SELECT * FROM local_models WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): LocalModel?

    @Query("DELETE FROM local_models WHERE id = :id")
    suspend fun deleteById(id: Int)
}