package com.alberto.firebase.data.network

import com.alberto.firebase.data.model.DeezerResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface DeezerApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String
    ): DeezerResponse
}


object RetrofitClient {
    private const val BASE_URL = "https://api.deezer.com/"

    val apiService: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }
}