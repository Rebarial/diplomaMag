package com.example.armodels.repository

import android.content.Context
import android.net.Uri
import com.example.armodels.network.ApiClient
import com.example.armodels.network.dto.Drawing
import com.example.armodels.network.dto.Model3D
import com.example.armodels.storage.AppDatabase
import com.example.armodels.storage.LocalModel
import com.example.armodels.storage.LocalModelStorage
import com.example.armodels.utils.mapHttpError
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ModelRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).localModelDao()
    private val fileStorage = LocalModelStorage(context)

    companion object {
        private const val GENERATION_MODE_IMPORTED = "imported"
    }

    fun observeLocalModels(): Flow<List<LocalModel>> = dao.observeAll()

    suspend fun getModels(): Result<List<Model3D>> = runCatching {
        ApiClient.service.getModels()
    }.mapHttpError()

    suspend fun getDrawings(): Result<List<Drawing>> = runCatching {
        ApiClient.service.getDrawings()
    }.mapHttpError()

    suspend fun deleteDrawing(id: Int): Result<Unit> = runCatching {
        ApiClient.service.deleteDrawing(id)
    }.mapHttpError()

    suspend fun getModelBytes(serverId: Int): Result<Pair<String, ByteArray>> = runCatching {
        val cached = dao.getByServerId(serverId)
        if (cached != null && fileStorage.exists(cached.localFilePath)) {
            return@runCatching Pair(cached.name, fileStorage.readBytes(cached.localFilePath))
        }
        val (local, bytes) = downloadAndCache(serverId)
        Pair(local.name, bytes)
    }

    suspend fun isModelSavedOnDevice(serverId: Int): Boolean {
        val existing = dao.getByServerId(serverId)
        return existing != null && fileStorage.exists(existing.localFilePath)
    }

    suspend fun getLocalModelBytes(localId: Int): Result<Pair<String, ByteArray>> = runCatching {
        val model = dao.getById(localId) ?: throw Exception("Модель не найдена")
        Pair(model.name, fileStorage.readBytes(model.localFilePath))
    }

    suspend fun importModel(name: String, uri: Uri): Result<LocalModel> = runCatching {
        val path = fileStorage.copyFromUri(uri)
        val model = LocalModel(
            name = name,
            localFilePath = path,
            serverId = null,
            generationMode = GENERATION_MODE_IMPORTED,
            createdAt = Instant.now().toString()
        )
        val id = dao.insert(model)
        model.copy(id = id.toInt())
    }

    suspend fun saveModelToDevice(serverId: Int): Result<LocalModel> = runCatching {
        val existing = dao.getByServerId(serverId)
        if (existing != null && fileStorage.exists(existing.localFilePath)) {
            return@runCatching existing
        }
        downloadAndCache(serverId).first
    }

    private suspend fun downloadAndCache(serverId: Int): Pair<LocalModel, ByteArray> {
        val meta = ApiClient.service.getModel(serverId)
        val fileUrl = meta.file_url ?: throw Exception("Файл модели недоступен")
        val bytes = ApiClient.service.downloadFile(fileUrl).body()?.bytes()
            ?: throw Exception("Пустой файл")
        val path = fileStorage.saveBytes(bytes, "server_${serverId}.glb")
        val local = LocalModel(
            name = meta.name,
            localFilePath = path,
            serverId = serverId,
            generationMode = meta.generation_mode,
            createdAt = Instant.now().toString()
        )
        return Pair(local.copy(id = dao.insert(local).toInt()), bytes)
    }

    suspend fun deleteModel(id: Int): Result<Unit> = runCatching {
        ApiClient.service.deleteModel(id)
    }.mapHttpError()

    suspend fun saveGeneratedLocalModel(name: String, glbBytes: ByteArray): Result<LocalModel> = runCatching {
        val path = fileStorage.saveBytes(glbBytes, "generated_${System.currentTimeMillis()}.glb")
        val model = LocalModel(
            name = name,
            localFilePath = path,
            serverId = null,
            generationMode = "advanced",
            createdAt = Instant.now().toString()
        )
        val id = dao.insert(model)
        model.copy(id = id.toInt())
    }

    suspend fun deleteLocalModel(localId: Int): Result<Unit> = runCatching {
        val model = dao.getById(localId) ?: return@runCatching
        fileStorage.delete(model.localFilePath)
        dao.deleteById(localId)
    }

    suspend fun getModel(id: Int): Result<Model3D> = runCatching {
        ApiClient.service.getModel(id)
    }.mapHttpError()
}