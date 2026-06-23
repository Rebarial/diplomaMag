package com.example.armodels.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.armodels.network.ApiClient
import com.example.armodels.network.dto.GenerationResult
import com.example.armodels.network.dto.Model3D
import com.example.armodels.network.dto.PrimitiveDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import com.example.armodels.utils.mapHttpError
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class GenerationRepository(private val context: Context) {

    suspend fun generateSimple(
        drawingName: String,
        modelName: String,
        imageUri: Uri
    ): Result<GenerationResult> = generate(drawingName, modelName, imageUri, ApiClient.generationService::generateSimple)

    suspend fun generateAdvanced(
        drawingName: String,
        modelName: String,
        imageUri: Uri
    ): Result<GenerationResult> = generate(drawingName, modelName, imageUri, ApiClient.generationService::generateAdvanced)

    suspend fun recognizeAdvanced(
        drawingName: String,
        imageUri: Uri
    ): Result<List<PrimitiveDto>> = runCatching {
        val file = prepareFilePart(imageUri)
        ApiClient.generationService.recognizeAdvanced(
            drawingName.toRequestBody("text/plain".toMediaType()),
            file
        )
    }.mapHttpError()

    suspend fun uploadGlb(modelName: String, glbBytes: ByteArray): Result<Model3D> = runCatching {
        val body = glbBytes.toRequestBody("model/gltf-binary".toMediaTypeOrNull() ?: "application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "model_${System.currentTimeMillis()}.glb", body)
        ApiClient.service.uploadModel(
            modelName.toRequestBody("text/plain".toMediaType()),
            part
        )
    }.mapHttpError()

    private suspend fun generate(
        drawingName: String,
        modelName: String,
        imageUri: Uri,
        apiCall: suspend (RequestBody, RequestBody, MultipartBody.Part) -> GenerationResult
    ): Result<GenerationResult> {
        return runCatching {
            val file = prepareFilePart(imageUri)
            apiCall(
                drawingName.toRequestBody("text/plain".toMediaType()),
                modelName.toRequestBody("text/plain".toMediaType()),
                file
            )
        }.mapHttpError()
    }

    private fun prepareFilePart(uri: Uri): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Не удалось открыть поток для файла")
        val bytes = inputStream.readBytes()

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaType()

        var filename = uri.lastPathSegment
        if (filename.isNullOrEmpty()) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && it.moveToFirst()) {
                    filename = it.getString(nameIndex)
                }
            }
        }

        if (filename.isNullOrEmpty() || !filename.contains('.')) {
            val ext = if (mimeType.contains("png")) "png" else "jpg"
            filename = "upload_${System.currentTimeMillis()}.$ext"
        }

        return MultipartBody.Part.createFormData("file", filename, bytes.toRequestBody(mediaType))
    }
}