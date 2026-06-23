package com.example.armodels.geometry

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

object GlbBuilder {

    fun buildGlb(primitives: List<PrimitiveObject>): ByteArray {
        if (primitives.isEmpty()) return emptyGlb()
        val meshes = primitives.map { PrimitiveMeshBuilder.build(it) }

        // --- Pack binary buffer ---
        val binOut = ByteArrayOutputStream()
        // bufferViewInfos: (byteOffset, byteLength, target)
        val bvInfos = mutableListOf<Triple<Int, Int, Int>>()

        for (mesh in meshes) {
            val posBytes = mesh.positions.toLeBytes()
            bvInfos.add(Triple(binOut.size(), posBytes.size, 34962))
            binOut.write(posBytes)

            val normBytes = mesh.normals.toLeBytes()
            bvInfos.add(Triple(binOut.size(), normBytes.size, 34962))
            binOut.write(normBytes)

            val idxBytes = mesh.indices.toLeBytes()
            bvInfos.add(Triple(binOut.size(), idxBytes.size, 34963))
            binOut.write(idxBytes)
        }

        val rawBin = binOut.toByteArray()
        val binPaddedLen = align4(rawBin.size)
        val binPadded = rawBin.copyOf(binPaddedLen)

        // --- Build JSON ---
        val json = buildJson(primitives, meshes, bvInfos, rawBin.size)
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val jsonPaddedLen = align4(jsonBytes.size)
        val jsonPadded = ByteArray(jsonPaddedLen) { i ->
            if (i < jsonBytes.size) jsonBytes[i] else 0x20
        }

        // --- Assemble GLB ---
        val totalLen = 12 + 8 + jsonPaddedLen + 8 + binPaddedLen
        val buf = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46546C67)   // magic "glTF"
        buf.putInt(2)            // version
        buf.putInt(totalLen)
        // JSON chunk
        buf.putInt(jsonPaddedLen)
        buf.putInt(0x4E4F534A)
        buf.put(jsonPadded)
        // BIN chunk
        buf.putInt(binPaddedLen)
        buf.putInt(0x004E4942)
        buf.put(binPadded)
        return buf.array()
    }

    private fun buildJson(
        primitives: List<PrimitiveObject>,
        meshes: List<MeshData>,
        bvInfos: List<Triple<Int, Int, Int>>,
        totalBinLen: Int
    ): String {
        val sb = StringBuilder()
        sb.append("""{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}]""")

        // meshes
        sb.append(""","meshes":[{"primitives":[""")
        for (i in primitives.indices) {
            if (i > 0) sb.append(",")
            val posAcc = i * 3; val normAcc = i * 3 + 1; val idxAcc = i * 3 + 2
            sb.append("""{"attributes":{"POSITION":$posAcc,"NORMAL":$normAcc},"indices":$idxAcc,"material":$i,"mode":4}""")
        }
        sb.append("]}]")

        // materials
        sb.append(""","materials":[""")
        for (i in primitives.indices) {
            if (i > 0) sb.append(",")
            val c = primitives[i].color
            val r = c.getOrElse(0) { 0.7f }.fmt()
            val g = c.getOrElse(1) { 0.7f }.fmt()
            val b = c.getOrElse(2) { 0.7f }.fmt()
            val a = c.getOrElse(3) { 1.0f }.fmt()
            sb.append("""{"pbrMetallicRoughness":{"baseColorFactor":[$r,$g,$b,$a],"metallicFactor":0.1,"roughnessFactor":0.8},"doubleSided":true}""")
        }
        sb.append("]")

        // accessors
        sb.append(""","accessors":[""")
        for (i in primitives.indices) {
            val mesh = meshes[i]
            val vertCount = mesh.positions.size / 3
            val idxCount = mesh.indices.size
            val pos = mesh.positions
            var mnX = Float.MAX_VALUE; var mnY = Float.MAX_VALUE; var mnZ = Float.MAX_VALUE
            var mxX = -Float.MAX_VALUE; var mxY = -Float.MAX_VALUE; var mxZ = -Float.MAX_VALUE
            for (vi in 0 until vertCount) {
                val x = pos[vi*3]; val y = pos[vi*3+1]; val z = pos[vi*3+2]
                if (x < mnX) mnX = x; if (x > mxX) mxX = x
                if (y < mnY) mnY = y; if (y > mxY) mxY = y
                if (z < mnZ) mnZ = z; if (z > mxZ) mxZ = z
            }
            val bv = i * 3
            if (i > 0) sb.append(",")
            sb.append("""{"bufferView":$bv,"byteOffset":0,"componentType":5126,"count":$vertCount,"type":"VEC3","min":[${mnX.fmt()},${mnY.fmt()},${mnZ.fmt()}],"max":[${mxX.fmt()},${mxY.fmt()},${mxZ.fmt()}]}""")
            sb.append(""",{"bufferView":${bv+1},"byteOffset":0,"componentType":5126,"count":$vertCount,"type":"VEC3"}""")
            sb.append(""",{"bufferView":${bv+2},"byteOffset":0,"componentType":5125,"count":$idxCount,"type":"SCALAR"}""")
        }
        sb.append("]")

        // bufferViews
        sb.append(""","bufferViews":[""")
        for ((idx, bv) in bvInfos.withIndex()) {
            if (idx > 0) sb.append(",")
            sb.append("""{"buffer":0,"byteOffset":${bv.first},"byteLength":${bv.second},"target":${bv.third}}""")
        }
        sb.append("]")

        sb.append(""","buffers":[{"byteLength":$totalBinLen}]}""")
        return sb.toString()
    }

    private fun emptyGlb(): ByteArray {
        val json = """{"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[]}]}"""
        val jsonBytes = json.toByteArray()
        val jsonPaddedLen = align4(jsonBytes.size)
        val jsonPadded = ByteArray(jsonPaddedLen) { i -> if (i < jsonBytes.size) jsonBytes[i] else 0x20 }
        val totalLen = 12 + 8 + jsonPaddedLen
        val buf = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46546C67); buf.putInt(2); buf.putInt(totalLen)
        buf.putInt(jsonPaddedLen); buf.putInt(0x4E4F534A); buf.put(jsonPadded)
        return buf.array()
    }

    private fun align4(n: Int) = (n + 3) and -4

    private fun Float.fmt(): String {
        val s = String.format(Locale.US, "%.6f", this)
        return if (s.contains('.')) s.trimEnd('0').trimEnd('.').ifEmpty { "0" } else s
    }

    private fun FloatArray.toLeBytes(): ByteArray {
        val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
        forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun IntArray.toLeBytes(): ByteArray {
        val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
        forEach { buf.putInt(it) }
        return buf.array()
    }
}
