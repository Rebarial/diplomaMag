package com.example.armodels.ui.generate

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import java.nio.ByteBuffer

@SuppressLint("ClickableViewAccessibility")
@Composable
fun InlineModelPreview(
    glbBytes: ByteArray,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sceneViewRef by remember { mutableStateOf<SceneView?>(null) }
    var modelLoaded by remember(glbBytes) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            sceneViewRef?.destroy()
            sceneViewRef = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Превью модели",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", modifier = Modifier.size(20.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        SceneView(ctx).also { sv ->
                            sv.setOnTouchListener { _, _ -> true }
                            sceneViewRef = sv
                            sv.post {
                                setupScene(sv, glbBytes)
                                modelLoaded = true
                            }
                        }
                    },
                    update = { sv ->
                        if (!modelLoaded) {
                            sv.post {
                                setupScene(sv, glbBytes)
                                modelLoaded = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!modelLoaded) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

private fun setupScene(sv: SceneView, glbBytes: ByteArray) {
    try {
        // Clear previous nodes
        sv.childNodes.toList().forEach { sv.removeChildNode(it) }

        setupLighting(sv)

        // Load model
        val model = sv.modelLoader.createModel(ByteBuffer.wrap(glbBytes)) ?: return
        val instance = sv.modelLoader.createInstance(model) ?: return
        val node = ModelNode(modelInstance = instance, scaleToUnits = 1f).apply { centerOrigin() }
        sv.addChildNode(node)

        // Profile view: camera on +X axis, Z is screen-up so shape height appears vertical.
        // lookAt is offset to upper-right so the model sits in the lower-left corner.
        sv.cameraNode.worldPosition = Float3(3.5f, 0f, 0f)
        sv.cameraNode.lookAt(Float3(0f, 1.0f, 1.0f), Float3(0f, 0f, 1f))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun setupLighting(sv: SceneView) {
    val engine = sv.engine
    val em = EntityManager.get()

    // Flat ambient via IndirectLight (L0 SH band, constant gray)
    val sh = floatArrayOf(1f, 1f, 1f)  // L0: R=1, G=1, B=1
    val ibl = IndirectLight.Builder()
        .irradiance(1, sh)
        .intensity(10_000f)
        .build(engine)
    sv.scene.indirectLight = ibl

    // Key light: warm, front-top
    em.create().also { e ->
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.97f, 0.9f)
            .intensity(90_000f)
            .direction(0.2f, -0.9f, -0.4f)
            .castShadows(false)
            .build(engine, e)
        sv.scene.addEntity(e)
    }

    // Fill light: cool, back-top
    em.create().also { e ->
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.5f, 0.6f, 0.85f)
            .intensity(25_000f)
            .direction(-0.3f, -0.4f, 0.9f)
            .castShadows(false)
            .build(engine, e)
        sv.scene.addEntity(e)
    }

    // Rim light: bottom, to avoid pitch-black undersides
    em.create().also { e ->
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.65f, 0.65f, 0.75f)
            .intensity(12_000f)
            .direction(0f, 0.95f, 0.2f)
            .castShadows(false)
            .build(engine, e)
        sv.scene.addEntity(e)
    }
}
