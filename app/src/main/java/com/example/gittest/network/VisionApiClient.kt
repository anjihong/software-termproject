// app/src/main/java/com/example/gittest/network/VisionApiClient.kt
package com.example.gittest.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object VisionApiClient {
    private const val BASE_URL = "https://vision.googleapis.com/"
    const val API_KEY = "AIzaSyDMirgaJb3U7UKFISasAVe0hfqXA6fpKs4"  // 발급받은 키를 여기에 넣으세요

    val service: VisionApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VisionApiService::class.java)
    }
}
