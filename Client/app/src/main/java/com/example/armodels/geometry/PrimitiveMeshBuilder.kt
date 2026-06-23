package com.example.armodels.geometry

import kotlin.math.*

data class MeshData(
    val positions: FloatArray,
    val normals: FloatArray,
    val indices: IntArray
)

object PrimitiveMeshBuilder {

    fun build(primitive: PrimitiveObject): MeshData {
        val (rawVerts, faces) = generateShape(primitive)
        val transformed = applyTransform(rawVerts, primitive.offset, primitive.rotation)
        val triangles = triangulateFaces(faces)
        return flattenWithNormals(transformed, triangles)
    }

    private fun generateShape(p: PrimitiveObject): Pair<List<FloatArray>, List<IntArray>> {
        val d = p.dimensions
        return when (p.type) {
            PrimitiveType.CUBOID   -> cuboid(d[0], d[1], d[2])
            PrimitiveType.PYRAMID  -> pyramid(d[0], d[1], d[2])
            PrimitiveType.CYLINDER -> cylinder(d[0], d[1])
            PrimitiveType.CONE     -> cone(d[0], d[1])
            PrimitiveType.SPHERE   -> sphere(d[0])
            PrimitiveType.PRISM    -> prism(d[0], d[1])
            PrimitiveType.TORUS    -> torus(d[0], d[1])
        }
    }

    // Cuboid: length, width, height
    private fun cuboid(l: Float, w: Float, h: Float): Pair<List<FloatArray>, List<IntArray>> {
        val v = listOf(
            floatArrayOf(0f, 0f, 0f), floatArrayOf(l, 0f, 0f),
            floatArrayOf(l, w, 0f),   floatArrayOf(0f, w, 0f),
            floatArrayOf(0f, 0f, h),  floatArrayOf(l, 0f, h),
            floatArrayOf(l, w, h),    floatArrayOf(0f, w, h)
        )
        val f = listOf(
            intArrayOf(0, 1, 2, 3), intArrayOf(4, 7, 6, 5),
            intArrayOf(0, 4, 5, 1), intArrayOf(2, 6, 7, 3),
            intArrayOf(1, 5, 6, 2), intArrayOf(3, 7, 4, 0)
        )
        return Pair(v, f)
    }

    // Pyramid: length, width, height
    private fun pyramid(l: Float, w: Float, h: Float): Pair<List<FloatArray>, List<IntArray>> {
        val v = listOf(
            floatArrayOf(0f, 0f, 0f),      floatArrayOf(l, 0f, 0f),
            floatArrayOf(l, w, 0f),         floatArrayOf(0f, w, 0f),
            floatArrayOf(l / 2f, w / 2f, h)
        )
        val f = listOf(
            intArrayOf(0, 3, 2, 1),
            intArrayOf(0, 1, 4), intArrayOf(1, 2, 4),
            intArrayOf(2, 3, 4), intArrayOf(3, 0, 4)
        )
        return Pair(v, f)
    }

    // Cylinder: radius, height
    private fun cylinder(r: Float, h: Float, seg: Int = 32): Pair<List<FloatArray>, List<IntArray>> {
        val v = mutableListOf<FloatArray>()
        val f = mutableListOf<IntArray>()
        for (i in 0 until seg) {
            val t = 2 * PI.toFloat() * i / seg
            v.add(floatArrayOf(r * cos(t), r * sin(t), 0f))
        }
        for (i in 0 until seg) {
            val t = 2 * PI.toFloat() * i / seg
            v.add(floatArrayOf(r * cos(t), r * sin(t), h))
        }
        f.add(IntArray(seg) { seg - 1 - it })
        f.add(IntArray(seg) { it + seg })
        for (i in 0 until seg) {
            val ni = (i + 1) % seg
            f.add(intArrayOf(i, ni, ni + seg, i + seg))
        }
        return Pair(v, f)
    }

    // Cone: radius, height
    private fun cone(r: Float, h: Float, seg: Int = 32): Pair<List<FloatArray>, List<IntArray>> {
        val v = mutableListOf<FloatArray>()
        val f = mutableListOf<IntArray>()
        for (i in 0 until seg) {
            val t = 2 * PI.toFloat() * i / seg
            v.add(floatArrayOf(r * cos(t), r * sin(t), 0f))
        }
        v.add(floatArrayOf(0f, 0f, h))
        val apex = seg
        f.add(IntArray(seg) { seg - 1 - it })
        for (i in 0 until seg) {
            val ni = (i + 1) % seg
            f.add(intArrayOf(i, ni, apex))
        }
        return Pair(v, f)
    }

    // Sphere: radius
    private fun sphere(r: Float, lat: Int = 16, lon: Int = 32): Pair<List<FloatArray>, List<IntArray>> {
        val v = mutableListOf<FloatArray>()
        val f = mutableListOf<IntArray>()
        v.add(floatArrayOf(0f, 0f, r))
        for (i in 1 until lat) {
            val phi = PI.toFloat() * i / lat
            for (j in 0 until lon) {
                val theta = 2 * PI.toFloat() * j / lon
                v.add(floatArrayOf(r * sin(phi) * cos(theta), r * sin(phi) * sin(theta), r * cos(phi)))
            }
        }
        v.add(floatArrayOf(0f, 0f, -r))
        val south = v.size - 1
        for (j in 0 until lon) {
            f.add(intArrayOf(0, j + 1, (j + 1) % lon + 1))
        }
        for (i in 0 until lat - 2) {
            for (j in 0 until lon) {
                val nj = (j + 1) % lon
                val a = 1 + i * lon + j
                val b = 1 + i * lon + nj
                val c = 1 + (i + 1) * lon + nj
                val d = 1 + (i + 1) * lon + j
                f.add(intArrayOf(a, b, c, d))
            }
        }
        val lr = 1 + (lat - 2) * lon
        for (j in 0 until lon) {
            val nj = (j + 1) % lon
            f.add(intArrayOf(south, lr + nj, lr + j))
        }
        return Pair(v, f)
    }

    // Prism: radius, height, sides=6
    private fun prism(r: Float, h: Float, sides: Int = 6): Pair<List<FloatArray>, List<IntArray>> {
        val v = mutableListOf<FloatArray>()
        val f = mutableListOf<IntArray>()
        for (i in 0 until sides) {
            val t = 2 * PI.toFloat() * i / sides
            v.add(floatArrayOf(r * cos(t), r * sin(t), 0f))
        }
        for (i in 0 until sides) {
            val t = 2 * PI.toFloat() * i / sides
            v.add(floatArrayOf(r * cos(t), r * sin(t), h))
        }
        f.add(IntArray(sides) { sides - 1 - it })
        f.add(IntArray(sides) { it + sides })
        for (i in 0 until sides) {
            val ni = (i + 1) % sides
            f.add(intArrayOf(i, ni, ni + sides, i + sides))
        }
        return Pair(v, f)
    }

    // Torus: majorR, minorR
    private fun torus(R: Float, r: Float, M: Int = 32, N: Int = 16): Pair<List<FloatArray>, List<IntArray>> {
        val v = mutableListOf<FloatArray>()
        val f = mutableListOf<IntArray>()
        for (i in 0 until M) {
            val phi = 2 * PI.toFloat() * i / M
            for (j in 0 until N) {
                val theta = 2 * PI.toFloat() * j / N
                v.add(floatArrayOf(
                    (R + r * cos(theta)) * cos(phi),
                    (R + r * cos(theta)) * sin(phi),
                    r * sin(theta)
                ))
            }
        }
        for (i in 0 until M) {
            val ni = (i + 1) % M
            for (j in 0 until N) {
                val nj = (j + 1) % N
                val a = i * N + j; val b = i * N + nj
                val c = ni * N + nj; val d = ni * N + j
                f.add(intArrayOf(a, b, c, d))
            }
        }
        return Pair(v, f)
    }

    // Apply offset + rotation (Rz @ Ry @ Rx, same as Python server)
    private fun applyTransform(verts: List<FloatArray>, offset: List<Float>, rotation: List<Float>): List<FloatArray> {
        val ox = offset.getOrElse(0) { 0f }
        val oy = offset.getOrElse(1) { 0f }
        val oz = offset.getOrElse(2) { 0f }
        val rx = Math.toRadians(rotation.getOrElse(0) { 0f }.toDouble()).toFloat()
        val ry = Math.toRadians(rotation.getOrElse(1) { 0f }.toDouble()).toFloat()
        val rz = Math.toRadians(rotation.getOrElse(2) { 0f }.toDouble()).toFloat()
        return verts.map { v ->
            val v1 = rotX(v, rx)
            val v2 = rotY(v1, ry)
            val v3 = rotZ(v2, rz)
            floatArrayOf(v3[0] + ox, v3[1] + oy, v3[2] + oz)
        }
    }

    private fun rotX(v: FloatArray, a: Float): FloatArray {
        val c = cos(a); val s = sin(a)
        return floatArrayOf(v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c)
    }

    private fun rotY(v: FloatArray, a: Float): FloatArray {
        val c = cos(a); val s = sin(a)
        return floatArrayOf(v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c)
    }

    private fun rotZ(v: FloatArray, a: Float): FloatArray {
        val c = cos(a); val s = sin(a)
        return floatArrayOf(v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2])
    }

    // Fan triangulation: polygon [v0,v1,...,vN] → triangles [v0,v1,v2],[v0,v2,v3],...
    private fun triangulateFaces(faces: List<IntArray>): List<IntArray> {
        val result = mutableListOf<IntArray>()
        for (face in faces) {
            if (face.size < 3) continue
            for (i in 1 until face.size - 1) {
                result.add(intArrayOf(face[0], face[i], face[i + 1]))
            }
        }
        return result
    }

    // Duplicate vertices per triangle for flat shading; sequential indices
    private fun flattenWithNormals(verts: List<FloatArray>, triangles: List<IntArray>): MeshData {
        val positions = FloatArray(triangles.size * 9)
        val normals   = FloatArray(triangles.size * 9)
        val indices   = IntArray(triangles.size * 3) { it }
        for ((ti, tri) in triangles.withIndex()) {
            val v0 = verts[tri[0]]; val v1 = verts[tri[1]]; val v2 = verts[tri[2]]
            val n = faceNormal(v0, v1, v2)
            val b = ti * 9
            positions[b]   = v0[0]; positions[b+1] = v0[1]; positions[b+2] = v0[2]
            positions[b+3] = v1[0]; positions[b+4] = v1[1]; positions[b+5] = v1[2]
            positions[b+6] = v2[0]; positions[b+7] = v2[1]; positions[b+8] = v2[2]
            for (k in 0..2) { normals[b+k] = n[k]; normals[b+3+k] = n[k]; normals[b+6+k] = n[k] }
        }
        return MeshData(positions, normals, indices)
    }

    private fun faceNormal(v0: FloatArray, v1: FloatArray, v2: FloatArray): FloatArray {
        val e1 = floatArrayOf(v1[0]-v0[0], v1[1]-v0[1], v1[2]-v0[2])
        val e2 = floatArrayOf(v2[0]-v0[0], v2[1]-v0[1], v2[2]-v0[2])
        val nx = e1[1]*e2[2] - e1[2]*e2[1]
        val ny = e1[2]*e2[0] - e1[0]*e2[2]
        val nz = e1[0]*e2[1] - e1[1]*e2[0]
        val len = sqrt(nx*nx + ny*ny + nz*nz)
        return if (len > 0f) floatArrayOf(nx/len, ny/len, nz/len) else floatArrayOf(0f, 0f, 1f)
    }
}
