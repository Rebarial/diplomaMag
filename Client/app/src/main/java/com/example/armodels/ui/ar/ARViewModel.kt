package com.example.armodels.ui.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.repository.ModelRepository
import com.example.armodels.utils.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ArLoadState {
    IDLE, LOADING, READY, PLACED, ERROR
}

data class ArUiState(
    val loadState: ArLoadState = ArLoadState.IDLE,
    val glbBytes: ByteArray? = null,
    val error: String? = null,
    val modelName: String = ""
)

class ARViewModel(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    fun loadModel(modelId: Int) = loadBytes { modelRepository.getModelBytes(modelId) }

    fun loadLocalModel(localId: Int) = loadBytes { modelRepository.getLocalModelBytes(localId) }

    private fun loadBytes(fetch: suspend () -> Result<Pair<String, ByteArray>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadState = ArLoadState.LOADING, error = null)
            val result = fetch()
            if (result.isSuccess) {
                val (name, bytes) = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    loadState = ArLoadState.READY,
                    glbBytes = bytes,
                    modelName = name
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    loadState = ArLoadState.ERROR,
                    error = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun onModelPlaced() {
        _uiState.value = _uiState.value.copy(loadState = ArLoadState.PLACED)
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(loadState = ArLoadState.READY, error = null)
    }
}