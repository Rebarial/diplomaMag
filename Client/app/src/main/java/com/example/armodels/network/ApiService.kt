package com.example.armodels.network

import com.example.armodels.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("auth/register/")
    suspend fun register(@Body request: LoginRequest): AuthResponse

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("models/")
    suspend fun getModels(): List<Model3D>

    @GET("models/{id}/")
    suspend fun getModel(@Path("id") id: Int): Model3D

    @DELETE("models/{id}/")
    suspend fun deleteModel(@Path("id") id: Int)

    @GET("drawings/")
    suspend fun getDrawings(): List<Drawing>

    @DELETE("drawings/{id}/")
    suspend fun deleteDrawing(@Path("id") id: Int)

    @Multipart
    @POST("models/upload/")
    suspend fun uploadModel(
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Model3D

    @Multipart
    @POST("generate/simple/")
    suspend fun generateSimple(
        @Part("drawing_name") drawingName: RequestBody,
        @Part("model_name") modelName: RequestBody,
        @Part file: MultipartBody.Part
    ): GenerationResult

    @Multipart
    @POST("generate/advanced/")
    suspend fun generateAdvanced(
        @Part("drawing_name") drawingName: RequestBody,
        @Part("model_name") modelName: RequestBody,
        @Part file: MultipartBody.Part
    ): GenerationResult

    @Multipart
    @POST("generate/advanced/")
    suspend fun recognizeAdvanced(
        @Part("drawing_name") drawingName: RequestBody,
        @Part file: MultipartBody.Part
    ): List<PrimitiveDto>

    @GET
    suspend fun downloadFile(@Url fileUrl: String): retrofit2.Response<okhttp3.ResponseBody>
}