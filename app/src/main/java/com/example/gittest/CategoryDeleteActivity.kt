package com.example.gittest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
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
            .setOnClickListener { startCategoryList("풍경") }
        findViewById<View>(R.id.category_person)
            .setOnClickListener { startCategoryList("사람") }
        findViewById<View>(R.id.category_animal)
            .setOnClickListener { startCategoryList("동물") }
        findViewById<View>(R.id.category_food)
            .setOnClickListener { startCategoryList("음식") }
        findViewById<View>(R.id.category_etc)
            .setOnClickListener { startCategoryList("기타") }
    }

    private fun startCategoryList(category: String) {
        Intent(this, CategoryListActivity::class.java).also {
            it.putExtra("category", category)
            startActivity(it)
        }
    }

    /** 사진 전체를 분류해서 Firestore에 저장 */
    private suspend fun classifyAllPhotos() {
        lifecycleScope.launch {
            Log.d("CategoryDelete", "classifyAllPhotos() 시작")
            val firestore = Firebase.firestore
            // 1) MediaStore에서 URI 목록 로드
            val uris = loadAllPhotoUris()
            Log.d("CategoryDelete", "URI 갯수: ${uris.size}")

            uris.forEach { uri ->
                Log.d("CategoryDelete", "분류 대상 URI: $uri")
                // 2) 이미 분류된 적 있으면 건너뛰기
                val photoId = uri.lastPathSegment ?: uri.toString().hashCode().toString()
                val docRef = firestore.collection("photoLabels")
                    .document(photoId)
                if (docRef.get().await().exists()) return@forEach

                // 3) Base64 인코딩
                val b64 = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }.let { Base64.encodeToString(it, Base64.NO_WRAP) }

                // 4) Vision API 호출
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

                // → 여기에 후처리: 내가 원하는 5개 카테고리에 맞춰 매핑
                val categories = listOf("Person", "Animal", "Landscape", "Food")
                val matched = categories.firstOrNull { cat ->
                    labels.any { it.contains(cat, ignoreCase = true) }
                }?:"기타" // 매칭된 게 없으면 기타

                // 5) Firestore에 저장
                docRef.set(mapOf(
                    "category"  to matched, //분류 결과
                    "labels"    to labels,  //원본 라벨
                    "timestamp" to System.currentTimeMillis()
                )).await()
            }

        }
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
