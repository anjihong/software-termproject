package com.example.gittest.network

import retrofit2.http.GET
import retrofit2.http.Query

// OWM One Call API (현재날씨)
interface OpenWeatherService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric", // 섭씨
        @Query("appid") apiKey: String
    ): WeatherResponse
}

// 간단 응답 모델 (필요한 필드만)
data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val dt: Long,       // Unix timestamp
    val name: String    // 지역 이름
)

data class Weather(val main: String, val description: String)
data class Main(val temp: Float, val humidity: Int)
