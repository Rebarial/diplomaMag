package com.example.armodels.geometry

import java.util.UUID

enum class PrimitiveType(
    val label: String,
    val dimensionLabels: List<String>,
    val defaultDimensions: List<Float>
) {
    CUBOID("Cuboid", listOf("Длина", "Ширина", "Высота"), listOf(5f, 5f, 5f)),
    PYRAMID("Pyramid", listOf("Длина", "Ширина", "Высота"), listOf(5f, 5f, 7f)),
    CYLINDER("Cylinder", listOf("Радиус", "Высота"), listOf(3f, 7f)),
    CONE("Cone", listOf("Радиус", "Высота"), listOf(3f, 7f)),
    SPHERE("Sphere", listOf("Радиус"), listOf(4f)),
    PRISM("Prism", listOf("Радиус", "Высота"), listOf(3f, 7f)),
    TORUS("Torus", listOf("Большой R", "Малый r"), listOf(5f, 2f));

    companion object {
        fun fromString(s: String): PrimitiveType =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: CUBOID
    }
}

data class PrimitiveObject(
    val id: String = UUID.randomUUID().toString(),
    val type: PrimitiveType,
    val dimensions: List<Float>,
    val offset: List<Float> = listOf(0f, 0f, 0f),
    val rotation: List<Float> = listOf(0f, 0f, 0f),
    val color: List<Float> = listOf(0.7f, 0.7f, 0.7f, 1.0f)
)