package com.example.armodels.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(
    private val tokenProvider: suspend () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Token $token")
                .build()
        } else chain.request()
        return chain.proceed(request)
    }
}