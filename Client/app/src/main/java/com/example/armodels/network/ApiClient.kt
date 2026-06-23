package com.example.armodels.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var tokenProvider: suspend () -> String? = { null }

    private val logging get() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor { tokenProvider() })
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Отдельный клиент для генерации — нейросеть может работать до 10 минут
    private val generationClient: OkHttpClient by lazy {
        client.newBuilder()
            .readTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    @Volatile private var _service: ApiService? = null
    @Volatile private var _generationService: ApiService? = null

    val service: ApiService
        get() = _service ?: error("ApiClient не инициализирован. Вызови setBaseUrl() перед использованием.")

    val generationService: ApiService
        get() = _generationService ?: error("ApiClient не инициализирован. Вызови setBaseUrl() перед использованием.")

    fun init(provider: suspend () -> String?) {
        tokenProvider = provider
    }

    fun setBaseUrl(url: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())

        _service = retrofit.client(client).build().create(ApiService::class.java)
        _generationService = retrofit.client(generationClient).build().create(ApiService::class.java)
    }
}
