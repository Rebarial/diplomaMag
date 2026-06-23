package com.example.armodels.ui.ar

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.example.armodels.storage.LightingMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.ar.core.Config
import com.google.ar.core.Plane
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.model.Model
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val INITIAL_MODEL_SCALE = 0.5f
private const val DRAG_ROTATION_SENSITIVITY = 0.5f
private const val INSTANT_PLACEMENT_DEPTH = 1.0f
private val MODEL_SCALE_RANGE = 0.01f..2.0f

private data class ScreenshotPreview(val thumbnail: Bitmap, val uri: Uri)

private fun axisAngle(axis: Float3, degrees: Float): Quaternion {
    val r = (degrees * (Math.PI / 180.0)).toFloat() / 2f
    val s = sin(r)
    return Quaternion(axis.x * s, axis.y * s, axis.z * s, cos(r))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ARScreen(
    navController: NavController,
    viewModel: ARViewModel,
    modelId: Int,
    isLocal: Boolean = false,
    lightingMode: LightingMode = LightingMode.ENVIRONMENTAL_HDR
) {
    val state by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Hide system bars (navigation + status); they reappear on swipe from edge
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val ctrl = WindowCompat.getInsetsController(window, view)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted && state.loadState == ArLoadState.IDLE) {
            if (isLocal) viewModel.loadLocalModel(modelId) else viewModel.loadModel(modelId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !cameraPermission.status.isGranted -> PermissionMessage(
                showRationale = cameraPermission.status.shouldShowRationale,
                onRequest = { cameraPermission.launchPermissionRequest() },
                modifier = Modifier.align(Alignment.Center)
            )

            state.loadState == ArLoadState.LOADING ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            state.loadState == ArLoadState.ERROR -> ErrorMessage(
                error = state.error ?: "Неизвестная ошибка",
                onRetry = {
                    if (isLocal) viewModel.loadLocalModel(modelId) else viewModel.loadModel(modelId)
                },
                modifier = Modifier.align(Alignment.Center)
            )

            state.loadState == ArLoadState.READY || state.loadState == ArLoadState.PLACED -> {
                val glbBytes = state.glbBytes
                if (glbBytes != null) {
                    ARSceneContent(
                        glbBytes = glbBytes,
                        lightingMode = lightingMode,
                        onModelPlaced = { viewModel.onModelPlaced() },
                        onReset = { viewModel.reset() },
                        isPlaced = state.loadState == ArLoadState.PLACED,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        TopAppBar(
            title = { Text(state.modelName.ifEmpty { "AR просмотр" }) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ARSceneContent(
    glbBytes: ByteArray,
    lightingMode: LightingMode,
    onModelPlaced: () -> Unit,
    onReset: () -> Unit,
    isPlaced: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sceneViewRef by remember { mutableStateOf<ARSceneView?>(null) }
    var model by remember { mutableStateOf<Model?>(null) }
    var currentAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var currentModelNode by remember { mutableStateOf<ModelNode?>(null) }
    var currentPivotNode by remember { mutableStateOf<Node?>(null) }

    var normalizedBaseScale by remember { mutableFloatStateOf(INITIAL_MODEL_SCALE) }
    var modelScale by remember { mutableFloatStateOf(1.0f) }
    var currentRotation by remember { mutableStateOf(Quaternion()) }

    var lockRotationX by remember { mutableStateOf(false) }
    var lockRotationY by remember { mutableStateOf(false) }
    var lockRotationZ by remember { mutableStateOf(true) }
    var lockPlacement by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var screenshotPreview by remember { mutableStateOf<ScreenshotPreview?>(null) }

    val isPlacedUpdated      by rememberUpdatedState(isPlaced)
    val lockPlacementUpdated by rememberUpdatedState(lockPlacement)
    val lockRotationXUpdated by rememberUpdatedState(lockRotationX)
    val lockRotationYUpdated by rememberUpdatedState(lockRotationY)
    val lockRotationZUpdated by rememberUpdatedState(lockRotationZ)

    // Auto-dismiss screenshot preview after 4 seconds
    LaunchedEffect(screenshotPreview) {
        if (screenshotPreview != null) {
            delay(4_000L)
            screenshotPreview = null
        }
    }

    fun takeScreenshot() {
        val sv = sceneViewRef ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, "Скриншоты доступны на Android 8+", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = Bitmap.createBitmap(sv.width, sv.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(sv, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                coroutineScope.launch(Dispatchers.IO) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "AR_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ARModels")
                        }
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        }
                        // Small thumbnail for preview (256px keeps memory low)
                        val thumb = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                        launch(Dispatchers.Main) {
                            screenshotPreview = ScreenshotPreview(thumb, uri)
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Не удалось сделать скриншот", Toast.LENGTH_SHORT).show()
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun resetModel() {
        sceneViewRef?.let { sv ->
            currentAnchorNode?.let { old ->
                old.detachAnchor()
                sv.childNodes -= old
                currentAnchorNode = null
                currentModelNode = null
                currentPivotNode = null
                currentRotation = Quaternion()
            }
        }
        modelScale = 1.0f
        normalizedBaseScale = INITIAL_MODEL_SCALE
        onReset()
        menuExpanded = false
    }

    Box(modifier = modifier) {
        ARScene(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPlaced) {
                    if (!isPlaced) return@pointerInput
                    // Custom gesture loop: pointer count determines mode.
                    // 1 finger → rotation,  2 fingers → pinch-to-zoom.
                    // awaitEachGesture restarts cleanly after every lift-all.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            if (!event.changes.any { it.pressed }) break
                            val pressed = event.changes.filter { it.pressed }

                            if (pressed.size == 1) {
                                val ch = pressed[0]
                                val drag = ch.position - ch.previousPosition
                                ch.consume()
                                val pivot = currentPivotNode ?: continue
                                if (!lockRotationYUpdated) {
                                    currentRotation = normalize(
                                        axisAngle(Float3(0f, 1f, 0f), drag.x * DRAG_ROTATION_SENSITIVITY)
                                        * currentRotation
                                    )
                                }
                                when {
                                    !lockRotationXUpdated ->
                                        currentRotation = normalize(
                                            currentRotation *
                                            axisAngle(Float3(1f, 0f, 0f), drag.y * DRAG_ROTATION_SENSITIVITY)
                                        )
                                    !lockRotationZUpdated ->
                                        currentRotation = normalize(
                                            currentRotation *
                                            axisAngle(Float3(0f, 0f, 1f), drag.y * DRAG_ROTATION_SENSITIVITY)
                                        )
                                }
                                pivot.quaternion = currentRotation

                            } else if (pressed.size >= 2) {
                                val p1 = pressed[0]; val p2 = pressed[1]
                                val prev = (p1.previousPosition - p2.previousPosition).getDistance()
                                val curr = (p1.position - p2.position).getDistance()
                                if (prev > 0f) {
                                    val newScale = (modelScale * (curr / prev)).coerceIn(MODEL_SCALE_RANGE)
                                    if (newScale != modelScale) {
                                        modelScale = newScale
                                        val s = normalizedBaseScale * newScale
                                        currentModelNode?.scale = Float3(s, s, s)
                                    }
                                }
                                pressed.forEach { it.consume() }
                            }
                        }
                    }
                },
            planeRenderer = true,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.focusMode = Config.FocusMode.AUTO
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }
                config.lightEstimationMode = when (lightingMode) {
                    LightingMode.ENVIRONMENTAL_HDR -> Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    LightingMode.AMBIENT_INTENSITY -> Config.LightEstimationMode.AMBIENT_INTENSITY
                    LightingMode.NONE              -> Config.LightEstimationMode.DISABLED
                }
            },
            onViewCreated = {
                sceneViewRef = this
                val buffer = java.nio.ByteBuffer.wrap(glbBytes)
                model = modelLoader.createModel(buffer)
                planeRenderer.planeRendererMode =
                    io.github.sceneview.ar.scene.PlaneRenderer.PlaneRendererMode.RENDER_ALL
                this.post {
                    cameraStream?.isDepthOcclusionEnabled = true
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, _ ->
                    if (isPlacedUpdated && lockPlacementUpdated) return@rememberOnGestureListener

                    val sv    = sceneViewRef ?: return@rememberOnGestureListener
                    val frame = sv.frame ?: return@rememberOnGestureListener

                    val planeHit = frame.hitTest(motionEvent.x, motionEvent.y)
                        .firstOrNull { hit ->
                            val trackable = hit.trackable
                            trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                        }
                    val anchor = (
                        planeHit?.createAnchorOrNull()
                            ?: frame.hitTestInstantPlacement(motionEvent.x, motionEvent.y, INSTANT_PLACEMENT_DEPTH)
                                .firstOrNull()?.createAnchorOrNull()
                    ) ?: return@rememberOnGestureListener

                    currentAnchorNode?.let { old ->
                        old.detachAnchor()
                        sv.childNodes -= old
                    }

                    val m = model ?: return@rememberOnGestureListener

                    val modelNode = ModelNode(
                        modelInstance = sv.modelLoader.createInstance(m)
                            ?: return@rememberOnGestureListener,
                        scaleToUnits = INITIAL_MODEL_SCALE
                    ).apply {
                        centerOrigin()
                    }
                    normalizedBaseScale = modelNode.scale.x
                    modelScale = 1.0f
                    currentModelNode = modelNode

                    val pivotNode = Node(engine = sv.engine).apply {
                        rotation = Float3(0f, 0f, 0f)
                        addChildNode(modelNode)
                    }
                    currentPivotNode = pivotNode
                    currentRotation = Quaternion()

                    val anchorNode = AnchorNode(engine = sv.engine, anchor = anchor).apply {
                        addChildNode(pivotNode)
                    }
                    sv.childNodes += anchorNode
                    currentAnchorNode = anchorNode
                    onModelPlaced()
                }
            )
        )

        if (!isPlaced) {
            Text(
                text = "Наведите на поверхность (стол, стену, грань детали) и тапните",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            FloatingActionButton(
                onClick = { takeScreenshot() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "Сфотографировать")
            }
        }

        // Screenshot mini-preview — bottom left, slides in/out (visible in both states)
        AnimatedVisibility(
            visible = screenshotPreview != null,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            screenshotPreview?.let { preview ->
                Card(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(preview.uri, "image/jpeg")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                        screenshotPreview = null
                    },
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Image(
                        bitmap = preview.thumbnail.asImageBitmap(),
                        contentDescription = "Открыть скриншот в галерее",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (isPlaced) {
            // Scale badge + Speed Dial — bottom right
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = menuExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpeedDialItem(
                            label = "Скриншот",
                            icon = Icons.Filled.PhotoCamera,
                            onClick = { takeScreenshot() }
                        )
                        SpeedDialItem(
                            label = "Убрать модель",
                            icon = Icons.Filled.Delete,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = { resetModel() }
                        )
                        SpeedDialToggle(
                            label = "Поворот ↔",
                            locked = lockRotationY,
                            onClick = { lockRotationY = !lockRotationY }
                        )
                        SpeedDialToggle(
                            label = "Наклон ↕X",
                            locked = lockRotationX,
                            onClick = {
                                val new = !lockRotationX
                                lockRotationX = new
                                if (!new) lockRotationZ = true
                            }
                        )
                        SpeedDialToggle(
                            label = "Крен ↕Z",
                            locked = lockRotationZ,
                            onClick = {
                                val new = !lockRotationZ
                                lockRotationZ = new
                                if (!new) lockRotationX = true
                            }
                        )
                        SpeedDialToggle(
                            label = "Позиция",
                            locked = lockPlacement,
                            onClick = { lockPlacement = !lockPlacement }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${"%.2f".format(modelScale)}×",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                FloatingActionButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = if (menuExpanded) Icons.Filled.Close else Icons.Filled.Menu,
                        contentDescription = if (menuExpanded) "Закрыть меню" else "Открыть меню"
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SpeedDialToggle(
    label: String,
    locked: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (locked)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (locked)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor
        ) {
            Icon(
                imageVector = if (locked) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PermissionMessage(
    showRationale: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (showRationale)
                "Для AR нужен доступ к камере. Пожалуйста, разрешите."
            else
                "Доступ к камере необходим для AR-режима.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequest) { Text("Разрешить камеру") }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text("Повторить") }
    }
}
