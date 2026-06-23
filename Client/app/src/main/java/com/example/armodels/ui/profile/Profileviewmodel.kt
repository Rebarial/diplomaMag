package com.example.armodels.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.network.dto.Drawing
import com.example.armodels.network.dto.Model3D
import com.example.armodels.repository.ModelRepository
import com.example.armodels.utils.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProfileTab { MODELS, DRAWINGS }

data class ProfileUiState(
    val activeTab: ProfileTab = ProfileTab.MODELS,

    val deletingDrawingId: Int? = null,
    // Модели
    val models: List<Model3D> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,

    // Чертежи
    val drawings: List<Drawing> = emptyList(),
    val isLoadingDrawings: Boolean = false,
    val drawingsError: String? = null,

    val savingModelId: Int? = null,

    val actionError: String? = null,
    val actionSuccess: String? = null
)

class ProfileViewModel(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadModels()
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        when (tab) {
            ProfileTab.MODELS -> if (_uiState.value.models.isEmpty()) loadModels()
            ProfileTab.DRAWINGS -> if (_uiState.value.drawings.isEmpty()) loadDrawings()
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModels = true, modelsError = null)
            val result = modelRepository.getModels()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    models = result.getOrThrow(),
                    isLoadingModels = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingModels = false,
                    modelsError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun loadDrawings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDrawings = true, drawingsError = null)
            val result = modelRepository.getDrawings()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    drawings = result.getOrThrow(),
                    isLoadingDrawings = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingDrawings = false,
                    drawingsError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun deleteDrawing(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingDrawingId = id, actionError = null)
            val result = modelRepository.deleteDrawing(id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    drawings = _uiState.value.drawings.filter { it.id != id },
                    deletingDrawingId = null,
                    actionSuccess = "Чертёж удалён"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    deletingDrawingId = null,
                    actionError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun deleteModel(id: Int) {
        viewModelScope.launch {
            val result = modelRepository.deleteModel(id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    models = _uiState.value.models.filter { it.id != id },
                    actionSuccess = "Модель удалена"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun saveModelToDevice(serverId: Int) {
        viewModelScope.launch {
            if (modelRepository.isModelSavedOnDevice(serverId)) {
                _uiState.value = _uiState.value.copy(
                    actionSuccess = "Модель уже на устройстве"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(savingModelId = serverId, actionError = null)
            val result = modelRepository.saveModelToDevice(serverId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    savingModelId = null,
                    actionSuccess = "Модель сохранена на устройство"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    savingModelId = null,
                    actionError = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionError = null, actionSuccess = null)
    }
}