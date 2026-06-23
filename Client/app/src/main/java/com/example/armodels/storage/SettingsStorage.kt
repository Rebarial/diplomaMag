package com.example.armodels.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

enum class LightingMode(val displayName: String) {
    ENVIRONMENTAL_HDR("HDR"),
    AMBIENT_INTENSITY("Ambient"),
    NONE("Без освещения")
}

enum class GenerationMode(val displayName: String) {
    SIMPLE("Simple"),
    ADVANCED("Advanced")
}

class SettingsStorage(
    private val context: Context,
    private val defaultServerUrl: String  // передаётся снаружи из R.string.server_base_url
) {

    companion object {
        private val KEY_LIGHTING = stringPreferencesKey("lighting_mode")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_GENERATION_MODE = stringPreferencesKey("generation_mode")
    }

    val lightingMode: Flow<LightingMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_LIGHTING] ?: LightingMode.ENVIRONMENTAL_HDR.name
        LightingMode.entries.find { it.name == raw } ?: LightingMode.ENVIRONMENTAL_HDR
    }

    suspend fun setLightingMode(mode: LightingMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_LIGHTING] = mode.name
        }
    }

    val defaultGenerationMode: Flow<GenerationMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_GENERATION_MODE] ?: GenerationMode.SIMPLE.name
        GenerationMode.entries.find { it.name == raw } ?: GenerationMode.SIMPLE
    }

    suspend fun setDefaultGenerationMode(mode: GenerationMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_GENERATION_MODE] = mode.name
        }
    }

    val serverUrl: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: defaultServerUrl
    }

    suspend fun setServerUrl(url: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url
        }
    }
}