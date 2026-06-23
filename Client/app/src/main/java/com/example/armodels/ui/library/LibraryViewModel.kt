package com.example.armodels.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.network.dto.Model3D
import com.example.armodels.repository.ModelRepository
import com.example.armodels.utils.ErrorMapper
import com.example.armodels.storage.LocalModel
import com.example.armodels.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DateFilter {
    ALL, TODAY, WEEK, MONTH
}

data class LibraryUiState(
    val isLoggedIn: Boolean = false,
    val username: String? = null,
    val serverModels: List<Model3D> = emptyList(),
    val localModels: List<LocalModel> = emptyList(),
    val isLoadingServer: Boolean = false,
    val serverError: String? = null,
    val importError: String? = null,
    val showImportDialog: Boolean = false,
    val searchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL
) {
    val filteredLocalModels: List<LocalModel>
        get() = localModels.filter { model ->
            matchesQuery(model.name) && matchesDate(model.createdAt)
        }

    val filteredServerModels: List<Model3D>
        get() = serverModels.filter { model ->
            matchesQuery(model.name) && matchesDate(model.created_at)
        }

    private fun matchesQuery(name: String): Boolean =
        searchQuery.isBlank() || name.contains(searchQuery.trim(), ignoreCase = true)

    private fun matchesDate(createdAt: String): Boolean {
        if (dateFilter == DateFilter.ALL) return true
        return try {
            val date = LocalDate.parse(createdAt.take(10))
            val now = LocalDate.now()
            when (dateFilter) {
                DateFilter.TODAY -> date == now
                DateFilter.WEEK  -> !date.isBefore(now.minusDays(7))
                DateFilter.MONTH -> !date.isBefore(now.minusDays(30))
                DateFilter.ALL   -> true
            }
        } catch (e: Exception) { true }
    }
}

class LibraryViewModel(
    private val modelRepository: ModelRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeLocalModels()
        checkAuthAndLoadServer()
    }

    private fun observeLocalModels() {
        viewModelScope.launch {
            modelRepository.observeLocalModels().collect { locals ->
                _uiState.value = _uiState.value.copy(localModels = locals)
            }
        }
    }

    private fun checkAuthAndLoadServer() {
        viewModelScope.launch {
            val token = tokenStorage.getToken()
            val username = tokenStorage.getUsername()
            _uiState.value = _uiState.value.copy(
                isLoggedIn = token != null,
                username = username
            )
            if (token != null) {
                loadServerModels()
            }
        }
    }

    fun loadServerModels() {
        viewModelScope.launch {
            val token = tokenStorage.getToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = false)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoadingServer = true, serverError = null)
            val result = modelRepository.getModels()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    serverModels = result.getOrThrow(),
                    isLoadingServer = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingServer = false,
                    serverError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onDateFilterChange(filter: DateFilter) {
        _uiState.value = _uiState.value.copy(dateFilter = filter)
    }

    fun deleteServerModel(id: Int) {
        viewModelScope.launch {
            val result = modelRepository.deleteModel(id)
            if (result.isSuccess) {
                loadServerModels()
            } else {
                _uiState.value = _uiState.value.copy(
                    serverError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun deleteLocalModel(localId: Int) {
        viewModelScope.launch {
            val result = modelRepository.deleteLocalModel(localId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    serverError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun importModel(name: String, uri: Uri) {
        viewModelScope.launch {
            val result = modelRepository.importModel(name, uri)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    importError = "Не удалось импортировать файл"
                )
            }
            _uiState.value = _uiState.value.copy(showImportDialog = false)
        }
    }

    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true, importError = null)
    }

    fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false, importError = null)
    }

    fun setImportError(message: String) {
        _uiState.value = _uiState.value.copy(importError = message, showImportDialog = false)
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearToken()
            _uiState.value = _uiState.value.copy(
                isLoggedIn = false,
                username = null,
                serverModels = emptyList(),
                serverError = null
            )
        }
    }

    fun onReturnFromLogin() {
        checkAuthAndLoadServer()
    }
}