// app/src/main/java/com/example/gittest/network/ApiModels.kt
package com.example.gittest.network

data class AnnotateRequest(val requests: List<AnnotateSingle>)
data class AnnotateSingle(val image: ImageContent, val features: List<Feature>)
data class ImageContent(val content: String)  // Base64 인코딩
data class Feature(val type: String = "LABEL_DETECTION", val maxResults: Int = 5)

data class AnnotateResponse(val responses: List<AnnotateSingleResponse>)
data class AnnotateSingleResponse(val labelAnnotations: List<LabelAnnotation>?)
data class LabelAnnotation(val description: String, val score: Float)
