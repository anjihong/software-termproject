// OpenWeatherClient.kt
package com.example.gittest.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenWeatherClient {
    private const val BASE_URL = "https://api.openweathermap.org/"
    const val API_KEY = "45cbc4ce6465d5def266e0f2127133b7" // 발급받은 키

    val service: OpenWeatherService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherService::class.java)
    }
}
