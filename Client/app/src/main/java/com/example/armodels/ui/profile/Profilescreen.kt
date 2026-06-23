package com.example.armodels.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.armodels.network.dto.Drawing
import com.example.armodels.network.dto.Model3D

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Показываем snackbar при успехе или ошибке
    LaunchedEffect(state.actionSuccess, state.actionError) {
        val message = state.actionSuccess ?: state.actionError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Вкладки ──────────────────────────────────────────────────────
            TabRow(selectedTabIndex = state.activeTab.ordinal) {
                Tab(
                    selected = state.activeTab == ProfileTab.MODELS,
                    onClick = { viewModel.selectTab(ProfileTab.MODELS) },
                    text = { Text("Модели") }
                )
                Tab(
                    selected = state.activeTab == ProfileTab.DRAWINGS,
                    onClick = { viewModel.selectTab(ProfileTab.DRAWINGS) },
                    text = { Text("Чертежи") }
                )
            }

            // ── Содержимое вкладки ────────────────────────────────────────────
            when (state.activeTab) {
                ProfileTab.MODELS -> ModelsTab(
                    state = state,
                    onDelete = viewModel::deleteModel,
                    onSaveToDevice = viewModel::saveModelToDevice,
                    onRefresh = viewModel::loadModels
                )
                ProfileTab.DRAWINGS -> DrawingsTab(
                    state = state,
                    onDelete = viewModel::deleteDrawing,
                    onRefresh = viewModel::loadDrawings
                )
            }
        }
    }
}

// ── Вкладка «Модели» ──────────────────────────────────────────────────────────

@Composable
private fun ModelsTab(
    state: ProfileUiState,
    onDelete: (Int) -> Unit,
    onSaveToDevice: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    when {
        state.isLoadingModels -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.modelsError != null -> {
            ErrorState(message = state.modelsError, onRetry = onRefresh)
        }
        state.models.isEmpty() -> {
            EmptyState(text = "Нет моделей на сервере")
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.models, key = { it.id }) { model ->
                    ServerModelCard(
                        model = model,
                        isSaving = state.savingModelId == model.id,
                        onDelete = { onDelete(model.id) },
                        onSaveToDevice = { onSaveToDevice(model.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ServerModelCard(
    model: Model3D,
    isSaving: Boolean,
    onDelete: () -> Unit,
    onSaveToDevice: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить модель?") },
            text = { Text("«${model.name}» будет удалена с сервера.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = model.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Режим: ${model.generation_mode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Создана: ${model.created_at.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Сохранить на устройство
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Сохранение…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    IconButton(onClick = onSaveToDevice) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Сохранить на устройство",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "На устройство",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Удалить
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Вкладка «Чертежи» ─────────────────────────────────────────────────────────

@Composable
private fun DrawingsTab(
    state: ProfileUiState,
    onDelete: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    when {
        state.isLoadingDrawings -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.drawingsError != null -> {
            ErrorState(message = state.drawingsError, onRetry = onRefresh)
        }
        state.drawings.isEmpty() -> {
            EmptyState(text = "Нет чертежей на сервере")
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.drawings, key = { it.id }) { drawing ->
                    DrawingCard(
                        drawing = drawing,
                        isDeleting = state.deletingDrawingId == drawing.id,
                        onDelete = { onDelete(drawing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingCard(
    drawing: Drawing,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    var showFullscreen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чертёж?") },
            text = { Text("«${drawing.name}» будет удалён с сервера.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showFullscreen) {
        AlertDialog(
            onDismissRequest = { showFullscreen = false },
            confirmButton = {
                TextButton(onClick = { showFullscreen = false }) { Text("Закрыть") }
            },
            title = { Text(drawing.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                AsyncImage(
                    model = drawing.file,
                    contentDescription = drawing.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showFullscreen = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Box {
                AsyncImage(
                    model = drawing.file,
                    contentDescription = drawing.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(
                text = drawing.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// ── Вспомогательные composable ────────────────────────────────────────────────

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}