package com.example.armodels.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.storage.GenerationMode
import com.example.armodels.storage.LightingMode
import com.example.armodels.storage.SettingsStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStorage: SettingsStorage
) : ViewModel() {

    val lightingMode: StateFlow<LightingMode> = settingsStorage.lightingMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LightingMode.ENVIRONMENTAL_HDR
    )

    fun setLightingMode(mode: LightingMode) {
        viewModelScope.launch {
            settingsStorage.setLightingMode(mode)
        }
    }

    val defaultGenerationMode: StateFlow<GenerationMode> = settingsStorage.defaultGenerationMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GenerationMode.SIMPLE
    )

    fun setDefaultGenerationMode(mode: GenerationMode) {
        viewModelScope.launch {
            settingsStorage.setDefaultGenerationMode(mode)
        }
    }

    val serverUrl: StateFlow<String> = settingsStorage.serverUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            settingsStorage.setServerUrl(url)
        }
    }
}