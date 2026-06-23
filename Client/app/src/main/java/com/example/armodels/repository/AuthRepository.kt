package com.example.armodels.repository

import com.example.armodels.network.ApiClient
import com.example.armodels.network.dto.LoginRequest
import com.example.armodels.storage.TokenStorage
import com.example.armodels.utils.mapHttpError

class AuthRepository(private val tokenStorage: TokenStorage) {

    suspend fun login(username: String, password: String): Result<String> =
        runCatching {
            val response = ApiClient.service.login(LoginRequest(username, password))
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(username)
            response.token
        }.mapHttpError()

    suspend fun register(username: String, password: String): Result<String> =
        runCatching {
            val response = ApiClient.service.register(LoginRequest(username, password))
            tokenStorage.saveToken(response.token)
            tokenStorage.saveUsername(username)
            response.token
        }.mapHttpError()

    suspend fun logout() {
        tokenStorage.clearToken()
    }

    suspend fun getToken(): String? = tokenStorage.getToken()
}