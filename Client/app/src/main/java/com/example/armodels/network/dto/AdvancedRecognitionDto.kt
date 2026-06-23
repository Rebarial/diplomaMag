package com.example.armodels.network.dto

data class PrimitiveDto(
    val type: String,
    val dimensions: List<Float>,
    val offset: List<Float> = listOf(0f, 0f, 0f),
    val rotation: List<Float> = listOf(0f, 0f, 0f),
    val color: List<Float> = listOf(0.7f, 0.7f, 0.7f, 1.0f)
)
