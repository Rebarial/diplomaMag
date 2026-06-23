package com.example.armodels.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class LocalModelStorage(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    fun saveBytes(bytes: ByteArray, filename: String = "${UUID.randomUUID()}.glb"): String {
        val file = File(modelsDir, filename)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun copyFromUri(uri: Uri, filename: String = "${UUID.randomUUID()}.glb"): String {
        val dest = File(modelsDir, filename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw Exception("Не удалось открыть файл")
        return dest.absolutePath
    }

    fun readBytes(path: String): ByteArray =
        File(path).readBytes()

    fun exists(path: String): Boolean =
        File(path).exists()

    fun delete(path: String) {
        File(path).delete()
    }
}