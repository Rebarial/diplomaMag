package com.example.armodels.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenStorage(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USERNAME_KEY = stringPreferencesKey("username")

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[USERNAME_KEY] = username }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.first()[TOKEN_KEY]

    suspend fun getUsername(): String? =
        context.dataStore.data.first()[USERNAME_KEY]

    suspend fun clearToken() {
        context.dataStore.edit {
            it.remove(TOKEN_KEY)
            it.remove(USERNAME_KEY)
        }
    }
}