package com.example.armodels.network.dto

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String
)