package com.alberto.firebase.data.network

import com.alberto.firebase.data.model.DeezerResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Definimos la ruta exacta de búsqueda de Deezer
interface DeezerApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String // En Deezer, la búsqueda se manda con la letra "q"
    ): DeezerResponse
}

// 2. Construimos el cliente que hará las peticiones
object RetrofitClient {
    private const val BASE_URL = "https://api.deezer.com/"

    val apiService: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Traduce el JSON gigante a tus clases de Kotlin
            .build()
            .create(DeezerApiService::class.java)
    }
}