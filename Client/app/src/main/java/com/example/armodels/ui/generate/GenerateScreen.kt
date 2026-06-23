package com.example.armodels.ui.generate

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.armodels.geometry.PrimitiveType
import com.example.armodels.navigation.Screen
import com.example.armodels.storage.GenerationMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    navController: NavController,
    viewModel: GenerateViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.navigateToLogin) {
        if (state.navigateToLogin) {
            viewModel.onNavigatedToLogin()
            navController.navigate(Screen.Login.route) { launchSingleTop = true }
        }
    }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("logged_in", false)
            ?.collect { didLogin ->
                if (didLogin) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("logged_in", false)
                    viewModel.onReturnFromLogin()
                }
            }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.onImageSelected(cameraImageUri)
    }

    if (state.showAddPrimitiveDialog) {
        AddPrimitiveDialog(
            onSelect = { viewModel.addPrimitive(it) },
            onDismiss = { viewModel.dismissAddDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая модель") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        // Scroll to bottom when preview first appears
        LaunchedEffect(state.previewGlbBytes) {
            if (state.previewGlbBytes != null) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Auth banner
            if (!state.isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Для создания модели нужна авторизация", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { navController.navigate(Screen.Login.route) { launchSingleTop = true } }) { Text("Войти") }
                    }
                }
            }

            // Mode selector
            Text("Режим генерации", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeButton("Simple", state.generationMode == GenerationMode.SIMPLE, { viewModel.onModeChange(GenerationMode.SIMPLE) }, Modifier.weight(1f))
                ModeButton("Advanced", state.generationMode == GenerationMode.ADVANCED, { viewModel.onModeChange(GenerationMode.ADVANCED) }, Modifier.weight(1f))
            }

            // Drawing name
            OutlinedTextField(
                value = state.drawingName,
                onValueChange = viewModel::onDrawingNameChange,
                label = { Text("Имя чертежа") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            // Model name (only needed for simple or for saving in advanced)
            if (state.generationMode == GenerationMode.SIMPLE || state.advancedPhase == AdvancedPhase.EDITING) {
                OutlinedTextField(
                    value = state.modelName,
                    onValueChange = viewModel::onModelNameChange,
                    label = { Text("Имя модели") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )
            }

            // Image picker
            Text("Изображение чертежа", style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state.selectedImageUri != null) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Чертёж",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Выберите или сфотографируйте чертёж")
                        Text("PNG или JPG, мин. 400×400, макс. 10 МБ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f), enabled = !state.isLoading) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
                OutlinedButton(onClick = {
                    val uri = createCameraUri(context)
                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                }, modifier = Modifier.weight(1f), enabled = !state.isLoading) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
            }

            // Error
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Result card (shown after successful save in both modes)
            if (state.result != null) {
                ResultCard(
                    modelName = state.result!!.model_name,
                    onOpenInAr = {
                        navController.navigate(Screen.AR.createRoute(state.result!!.model_id)) {
                            popUpTo(Screen.Library.route)
                        }
                    }
                )
            }

            // Action area
            if (state.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(state.loadingMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.generationMode == GenerationMode.SIMPLE) {
                SimpleModeActions(state, viewModel, context)
            } else {
                AdvancedModeContent(state, viewModel)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SimpleModeActions(state: GenerateUiState, viewModel: GenerateViewModel, context: Context) {
    if (state.result != null) {
        Button(onClick = { viewModel.regenerate(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Перегенерировать")
        }
    } else {
        Button(onClick = { viewModel.generate(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Сгенерировать")
        }
    }
}

@Composable
private fun AdvancedModeContent(state: GenerateUiState, viewModel: GenerateViewModel) {
    val context = LocalContext.current
    when (state.advancedPhase) {
        AdvancedPhase.NONE, AdvancedPhase.RECOGNIZING -> {
            Button(
                onClick = { viewModel.recognize(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.advancedPhase == AdvancedPhase.NONE
            ) {
                Text("Распознать параметры")
            }
        }
        AdvancedPhase.EDITING, AdvancedPhase.SAVING -> {
            AdvancedEditor(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun AdvancedEditor(state: GenerateUiState, viewModel: GenerateViewModel) {
    // Primitives list
    Text(
        "Объекты модели (${state.primitives.size})",
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(4.dp))

    state.primitives.forEachIndexed { index, primitive ->
        PrimitiveEditorCard(
            index = index,
            primitive = primitive,
            onUpdate = { updated -> viewModel.updatePrimitive(index, updated) },
            onDelete = { viewModel.removePrimitive(index) }
        )
        Spacer(Modifier.height(8.dp))
    }

    // Add primitive button
    OutlinedButton(
        onClick = { viewModel.showAddDialog() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Добавить объект")
    }

    Spacer(Modifier.height(8.dp))

    // Action buttons
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { viewModel.generatePreview() },
            modifier = Modifier.weight(1f),
            enabled = state.primitives.isNotEmpty() && state.advancedPhase == AdvancedPhase.EDITING
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (state.previewGlbBytes == null) "Превью" else "Обновить")
        }
        Button(
            onClick = { viewModel.saveModel() },
            modifier = Modifier.weight(1f),
            enabled = state.primitives.isNotEmpty() && state.advancedPhase == AdvancedPhase.EDITING
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Сохранить")
        }
    }

    // Re-recognize button
    TextButton(
        onClick = { viewModel.onModeChange(GenerationMode.ADVANCED) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("← Распознать заново")
    }

    // Inline 3D preview — at the bottom so appearing doesn't shift content above
    if (state.previewGlbBytes != null) {
        Spacer(Modifier.height(4.dp))
        InlineModelPreview(
            glbBytes = state.previewGlbBytes,
            onRefresh = { viewModel.generatePreview() }
        )
    }
}

@Composable
private fun AddPrimitiveDialog(onSelect: (PrimitiveType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить объект") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PrimitiveType.entries.forEach { type ->
                    TextButton(
                        onClick = { onSelect(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Button(
        onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    ) { Text(label) }
}

@Composable
private fun ResultCard(modelName: String, onOpenInAr: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Модель сохранена!", style = MaterialTheme.typography.titleMedium)
        Text(modelName, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onOpenInAr, modifier = Modifier.fillMaxWidth()) {
            Text("Открыть в AR")
        }
    }
}
