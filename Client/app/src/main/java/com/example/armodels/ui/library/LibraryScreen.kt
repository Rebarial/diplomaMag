package com.example.armodels.ui.library

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.armodels.navigation.Screen
import com.example.armodels.network.dto.Model3D
import com.example.armodels.storage.LocalModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Боковое меню (drawer)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Модальное меню «Создать»
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("logged_in", false)
            ?.collect { didLogin ->
                if (didLogin) viewModel.onReturnFromLogin()
            }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = context.contentResolver.query(
                uri, null, null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: uri.lastPathSegment ?: ""

            if (!fileName.endsWith(".glb", ignoreCase = true)) {
                viewModel.setImportError("Поддерживаются только файлы .glb")
            } else {
                pendingUri = uri
                viewModel.showImportDialog()
            }
        }
    }

    if (state.showImportDialog) {
        ImportNameDialog(
            onConfirm = { name ->
                pendingUri?.let { viewModel.importModel(name, it) }
                pendingUri = null
            },
            onDismiss = {
                viewModel.hideImportDialog()
                pendingUri = null
            },
            error = state.importError
        )
    }

    if (state.importError != null && !state.showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            title = { Text("Неверный формат") },
            text = { Text(state.importError!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.hideImportDialog() }) { Text("Ок") }
            }
        )
    }

    // Модальное меню «Создать»
    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = createSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Добавить модель",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Column {
                            Text("Генерация на основе чертежа", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Создать 3D-модель из фото чертежа",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { createSheetState.hide() }.invokeOnCompletion {
                            showCreateSheet = false
                            navController.navigate(Screen.Generate.route)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Column {
                            Text("Импорт", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Загрузить .glb файл с устройства",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { createSheetState.hide() }.invokeOnCompletion {
                            showCreateSheet = false
                            filePicker.launch("*/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Drawer (боковое меню) ─────────────────────────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                ) {
                    // Заголовок
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Меню",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                        }
                    }

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Блок пользователя ─────────────────────────────────────
                    val username = state.username
                    if (state.isLoggedIn && username != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Вы вошли",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Профиль (если авторизован) или Войти (если нет)
                    if (state.isLoggedIn) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            label = { Text("Профиль") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }.invokeOnCompletion {
                                    navController.navigate(Screen.Profile.route)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    } else {
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text(
                                    text = "Войти / Зарегистрироваться",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }.invokeOnCompletion {
                                    navController.navigate(Screen.Login.route)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    // Настройки
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }.invokeOnCompletion {
                                navController.navigate(Screen.Settings.route)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )


                    // Кнопка выхода — только если авторизован
                    if (state.isLoggedIn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    Icons.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            label = {
                                Text(
                                    text = "Выйти",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }.invokeOnCompletion {
                                    viewModel.logout()
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Мои модели") },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateSheet = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Создать") }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ── Поисковая строка ─────────────────────────────────────────
                TextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Поиск по названию…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.large
                )

                // ── Фильтр по дате ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DateFilter.ALL to "Все",
                        DateFilter.TODAY to "Сегодня",
                        DateFilter.WEEK to "Неделя",
                        DateFilter.MONTH to "Месяц"
                    ).forEach { (filter, label) ->
                        FilterChip(
                            selected = state.dateFilter == filter,
                            onClick = { viewModel.onDateFilterChange(filter) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ── Список моделей ───────────────────────────────────────────
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.filteredLocalModels.isNotEmpty()) {
                            item {
                                Text(
                                    text = "На устройстве",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(state.filteredLocalModels, key = { "local_${it.id}" }) { model ->
                                LocalModelCard(
                                    model = model,
                                    onOpenInAr = {
                                        navController.navigate(Screen.ARLocal.createRoute(model.id))
                                    },
                                    onDelete = { viewModel.deleteLocalModel(model.id) }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "С сервера",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    top = if (state.filteredLocalModels.isNotEmpty()) 8.dp else 0.dp
                                )
                            )
                        }

                        when {
                            !state.isLoggedIn -> {
                                item {
                                    NotLoggedInBanner(
                                        onLoginClick = { navController.navigate(Screen.Login.route) }
                                    )
                                }
                            }
                            state.isLoadingServer -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                }
                            }
                            state.serverError != null -> {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = state.serverError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        TextButton(onClick = viewModel::loadServerModels) {
                                            Text("Повторить")
                                        }
                                    }
                                }
                            }
                            state.filteredServerModels.isEmpty() -> {
                                item {
                                    Text(
                                        text = if (state.searchQuery.isNotBlank() || state.dateFilter != DateFilter.ALL)
                                            "Ничего не найдено"
                                        else
                                            "Нет моделей на сервере",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                items(state.filteredServerModels, key = { "server_${it.id}" }) { model ->
                                    ServerModelCard(
                                        model = model,
                                        onOpenInAr = {
                                            navController.navigate(Screen.AR.createRoute(model.id))
                                        },
                                        onDelete = { viewModel.deleteServerModel(model.id) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }

                    if (!state.isLoadingServer &&
                        state.filteredLocalModels.isEmpty() &&
                        state.filteredServerModels.isEmpty() &&
                        !state.isLoggedIn &&
                        state.searchQuery.isBlank() &&
                        state.dateFilter == DateFilter.ALL
                    ) {
                        Text(
                            text = "Нет сохранённых моделей.\nВойдите чтобы загрузить модели с сервера,\nили импортируйте .glb файл.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotLoggedInBanner(onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Войдите, чтобы видеть модели с сервера",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onLoginClick) {
                Text("Войти / Зарегистрироваться")
            }
        }
    }
}

@Composable
private fun ImportNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    error: String?
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Название модели") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Введите название") },
                    singleLine = true
                )
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Импортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun LocalModelCard(
    model: LocalModel,
    onOpenInAr: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить модель?") },
            text = { Text("«${model.name}» будет удалена с устройства.") },
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = model.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (model.generationMode == "imported") "Импортирована"
                else "Режим: ${model.generationMode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Создана: ${model.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenInAr, modifier = Modifier.weight(1f)) {
                    Text("Открыть в AR")
                }
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

@Composable
private fun ServerModelCard(
    model: Model3D,
    onOpenInAr: () -> Unit,
    onDelete: () -> Unit
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenInAr, modifier = Modifier.weight(1f)) {
                    Text("Открыть в AR")
                }
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