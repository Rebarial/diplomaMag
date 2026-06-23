package com.example.armodels.ui.generate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.geometry.GlbBuilder
import com.example.armodels.geometry.PrimitiveObject
import com.example.armodels.geometry.PrimitiveType
import com.example.armodels.network.dto.GenerationResult
import com.example.armodels.repository.GenerationRepository
import com.example.armodels.repository.ModelRepository
import com.example.armodels.storage.GenerationMode
import com.example.armodels.storage.TokenStorage
import com.example.armodels.utils.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AdvancedPhase { NONE, RECOGNIZING, EDITING, SAVING }

data class GenerateUiState(
    val isLoggedIn: Boolean = true,
    val drawingName: String = "",
    val modelName: String = "",
    val selectedImageUri: Uri? = null,
    val generationMode: GenerationMode = GenerationMode.SIMPLE,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val error: String? = null,
    val result: GenerationResult? = null,
    val navigateToLogin: Boolean = false,
    // Advanced mode
    val advancedPhase: AdvancedPhase = AdvancedPhase.NONE,
    val primitives: List<PrimitiveObject> = emptyList(),
    val previewGlbBytes: ByteArray? = null,
    val showAddPrimitiveDialog: Boolean = false
)

class GenerateViewModel(
    private val generationRepository: GenerationRepository,
    private val modelRepository: ModelRepository,
    private val tokenStorage: TokenStorage,
    initialGenerationMode: GenerationMode = GenerationMode.SIMPLE
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState(generationMode = initialGenerationMode))
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasToken = tokenStorage.getToken() != null
            _uiState.value = _uiState.value.copy(isLoggedIn = hasToken)
        }
    }

    fun onReturnFromLogin() {
        viewModelScope.launch {
            val hasToken = tokenStorage.getToken() != null
            _uiState.value = _uiState.value.copy(isLoggedIn = hasToken)
        }
    }

    fun onNavigatedToLogin() {
        _uiState.value = _uiState.value.copy(navigateToLogin = false)
    }

    fun onDrawingNameChange(value: String) {
        _uiState.value = _uiState.value.copy(drawingName = value, error = null)
    }

    fun onModelNameChange(value: String) {
        _uiState.value = _uiState.value.copy(modelName = value, error = null)
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri, error = null)
    }

    fun onModeChange(mode: GenerationMode) {
        _uiState.value = _uiState.value.copy(
            generationMode = mode,
            advancedPhase = AdvancedPhase.NONE,
            primitives = emptyList(),
            error = null,
            result = null
        )
    }

    // ---- Simple mode ----

    fun generate(context: Context) {
        if (!validate(_uiState.value)) return
        viewModelScope.launch {
            if (tokenStorage.getToken() == null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = false, navigateToLogin = true)
                return@launch
            }
            executeSimpleGeneration()
        }
    }

    fun regenerate(context: Context) {
        val prevResult = _uiState.value.result ?: return
        val drawingName = _uiState.value.drawingName
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            modelRepository.deleteModel(prevResult.model_id)
            modelRepository.getDrawings().getOrNull()
                ?.find { it.name == drawingName }
                ?.let { modelRepository.deleteDrawing(it.id) }
            executeSimpleGeneration()
        }
    }

    private suspend fun executeSimpleGeneration() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, loadingMessage = "Генерация модели, подождите…", error = null, result = null)
        val result = generationRepository.generateSimple(
            drawingName = state.drawingName,
            modelName = state.modelName,
            imageUri = state.selectedImageUri!!
        )
        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(isLoading = false, result = result.getOrThrow())
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = ErrorMapper.map(result.exceptionOrNull()?.message)
            )
        }
    }

    // ---- Advanced mode ----

    fun recognize(context: Context) {
        if (!validateForRecognition(_uiState.value)) return
        viewModelScope.launch {
            if (tokenStorage.getToken() == null) {
                _uiState.value = _uiState.value.copy(isLoggedIn = false, navigateToLogin = true)
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Распознавание параметров…",
                error = null,
                advancedPhase = AdvancedPhase.RECOGNIZING
            )
            val result = generationRepository.recognizeAdvanced(
                drawingName = _uiState.value.drawingName,
                imageUri = _uiState.value.selectedImageUri!!
            )
            if (result.isSuccess) {
                val dtos = result.getOrThrow()
                val primitives = dtos.map { dto ->
                    PrimitiveObject(
                        type = PrimitiveType.fromString(dto.type),
                        dimensions = dto.dimensions,
                        offset = dto.offset,
                        rotation = dto.rotation,
                        color = dto.color
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    advancedPhase = AdvancedPhase.EDITING,
                    primitives = primitives
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    advancedPhase = AdvancedPhase.NONE,
                    error = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun updatePrimitive(index: Int, updated: PrimitiveObject) {
        val list = _uiState.value.primitives.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _uiState.value = _uiState.value.copy(primitives = list)
        }
    }

    fun removePrimitive(index: Int) {
        val list = _uiState.value.primitives.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(primitives = list)
        }
    }

    fun addPrimitive(type: PrimitiveType) {
        val newPrimitive = PrimitiveObject(
            type = type,
            dimensions = type.defaultDimensions.toList()
        )
        _uiState.value = _uiState.value.copy(
            primitives = _uiState.value.primitives + newPrimitive,
            showAddPrimitiveDialog = false
        )
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddPrimitiveDialog = true)
    }

    fun dismissAddDialog() {
        _uiState.value = _uiState.value.copy(showAddPrimitiveDialog = false)
    }

    fun generatePreview() {
        val primitives = _uiState.value.primitives
        if (primitives.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadingMessage = "Генерация превью…")
            val glb = withContext(Dispatchers.Default) { GlbBuilder.buildGlb(primitives) }
            _uiState.value = _uiState.value.copy(isLoading = false, previewGlbBytes = glb)
        }
    }

    fun saveModel() {
        val state = _uiState.value
        if (state.primitives.isEmpty()) return
        val modelNameVal = state.modelName.trim()
        if (modelNameVal.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите имя модели")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Сохранение модели на сервере…",
                advancedPhase = AdvancedPhase.SAVING
            )
            val glb = withContext(Dispatchers.Default) { GlbBuilder.buildGlb(state.primitives) }
            val result = generationRepository.uploadGlb(modelNameVal, glb)
            if (result.isSuccess) {
                val model = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    advancedPhase = AdvancedPhase.EDITING,
                    result = GenerationResult(
                        model_id = model.id,
                        model_name = model.name,
                        file_url = model.file_url ?: "",
                        generation_mode = model.generation_mode
                    )
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    advancedPhase = AdvancedPhase.EDITING,
                    error = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    private fun validate(state: GenerateUiState): Boolean {
        val drawingRegex = Regex("^[A-Za-z0-9_-]{1,50}$")
        val modelRegex = Regex("^[A-Za-z0-9_ -]{1,50}$")
        return when {
            !drawingRegex.matches(state.drawingName) -> {
                _uiState.value = _uiState.value.copy(error = "Имя чертежа: 1–50 символов, только A-Z, 0-9, _ -")
                false
            }
            !modelRegex.matches(state.modelName) -> {
                _uiState.value = _uiState.value.copy(error = "Имя модели: 1–50 символов, только A-Z, 0-9, _ -")
                false
            }
            state.selectedImageUri == null -> {
                _uiState.value = _uiState.value.copy(error = "Выберите изображение чертежа")
                false
            }
            else -> true
        }
    }

    private fun validateForRecognition(state: GenerateUiState): Boolean {
        val drawingRegex = Regex("^[A-Za-z0-9_-]{1,50}$")
        return when {
            !drawingRegex.matches(state.drawingName) -> {
                _uiState.value = _uiState.value.copy(error = "Имя чертежа: 1–50 символов, только A-Z, 0-9, _ -")
                false
            }
            state.selectedImageUri == null -> {
                _uiState.value = _uiState.value.copy(error = "Выберите изображение чертежа")
                false
            }
            else -> true
        }
    }
}
