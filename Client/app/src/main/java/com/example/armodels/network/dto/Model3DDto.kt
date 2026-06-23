package com.example.armodels.network.dto

data class Model3D(
    val id: Int,
    val name: String,
    val file_url: String? = null,
    val generation_mode: String,
    val created_at: String
)

data class GenerationResult(
    val model_id: Int,
    val model_name: String,
    val file_url: String,
    val generation_mode: String
)