package com.example.armodels.network.dto

data class Drawing(
    val id: Int,
    val name: String,
    val uploaded_at: String,
    val file: String? = null
)