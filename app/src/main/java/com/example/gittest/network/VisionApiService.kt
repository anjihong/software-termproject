// app/src/main/java/com/example/gittest/network/VisionApiService.kt
package com.example.gittest.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface VisionApiService {
    @POST("v1/images:annotate")
    suspend fun annotate(
        @Query("key") apiKey: String,
        @Body request: AnnotateRequest
    ): AnnotateResponse
}
