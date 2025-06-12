package com.example.gittest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.gittest.network.AnnotateRequest
import com.example.gittest.network.AnnotateSingle
import com.example.gittest.network.Feature
import com.example.gittest.network.ImageContent
import com.example.gittest.network.VisionApiClient
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CategoryDeleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_delete)

        // 1) 홈 버튼: 이전(MainActivity)으로 돌아가기
        findViewById<ImageButton>(R.id.btn_home)
            .setOnClickListener { finish() }

        // 2) 휴지통 버튼: TrashPreviewActivity 로 이동
        findViewById<ImageButton>(R.id.btn_trash)
            .setOnClickListener {
                startActivity(
                    Intent(this, TrashPreviewActivity::class.java)
                        .apply {
                            // 필요하다면 putParcelableArrayListExtra(...) 로 URI 리스트 전달
                        }
                )
            }
        findViewById<Button>(R.id.btn_ai_delete)
            .setOnClickListener {
                startActivity(
                Intent(this, SimilarPhotoActivity::class.java)
            ) }

        // 3) 화면 로드되자마자 분류 자동 시작
        lifecycleScope.launch(Dispatchers.IO) {
            classifyAllPhotos()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CategoryDeleteActivity,
                    "사진 분류 완료!",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // 3) 카테고리별 리스트 화면으로 이동
        findViewById<View>(R.id.category_landscape)
            .setOnClickListener { startCategoryList("landscape") }
        findViewById<View>(R.id.category_person)
            .setOnClickListener { startCategoryList("person") }
        findViewById<View>(R.id.category_animal)
            .setOnClickListener { startCategoryList("animal") }
        findViewById<View>(R.id.category_food)
            .setOnClickListener { startCategoryList("food") }
        findViewById<View>(R.id.category_etc)
            .setOnClickListener { startCategoryList("etc") }
    }

    private fun startCategoryList(category: String) {
        Intent(this, CategoryListActivity::class.java).also {
            it.putExtra("category", category)
            startActivity(it)
        }
    }

    /** 사진 전체를 분류해서 Firestore에 저장 */
    private suspend fun classifyAllPhotos() {
        val mapping = loadCategoryMapping(this@CategoryDeleteActivity)

        val firestore = Firebase.firestore
        val uris = loadAllPhotoUris()
        Log.d("CategoryDelete", "URI 갯수: ${uris.size}")

        for (uri in uris) {
            val photoId = uri.lastPathSegment ?: uri.toString().hashCode().toString()
            val docRef = firestore.collection("photoLabels").document(photoId)
            if (docRef.get().await().exists()) continue

            val b64 = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            }.let { Base64.encodeToString(it, Base64.NO_WRAP) }

            val request = AnnotateRequest(
                listOf(AnnotateSingle(ImageContent(b64), listOf(Feature())))
            )

            val labels = VisionApiClient.service
                .annotate(VisionApiClient.API_KEY, request)
                .responses
                .firstOrNull()
                ?.labelAnnotations
                ?.map { it.description }
                ?: emptyList()

            val matched = getCategoryFromVisionTags(labels, mapping)

            docRef.set(mapOf(
                "category"  to matched,
                "labels"    to labels,
                "timestamp" to System.currentTimeMillis(),
                "uri"       to uri.toString()
            )).await()

            Log.d("CategoryDelete", "분류 저장 완료: $matched - $uri")
        }
    }


    private suspend fun loadCategoryMapping(context: Context): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("label_to_category.json").bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
            com.google.gson.Gson().fromJson(json, type)
        }
    }

    private fun getCategoryFromVisionTags(
        visionTags: List<String>,
        mapping: Map<String, List<String>>
    ): String {
        for (tag in visionTags) {
            val normalized = tag.lowercase()
            for ((category, keywords) in mapping) {
                if (keywords.any { it.lowercase() == normalized }) {
                    return category
                }
            }
        }
        return "etc"
    }


    /** MediaStore에서 모든 사진 URI를 최신순으로 가져옵니다 */
    private suspend fun loadAllPhotoUris(): List<Uri> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Uri>()
        val proj = arrayOf(MediaStore.Images.Media._ID)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            proj, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        cursor?.use {
            val idx = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idx)
                list += ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
            }
        }
        list
    }


}
