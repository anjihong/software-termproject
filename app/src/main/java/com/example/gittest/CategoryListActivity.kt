package com.example.gittest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CategoryListActivity : AppCompatActivity() {

    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var photoCardAdapter: PhotoCardAdapter
    private val photoUris = mutableListOf<Uri>()
    private val photosMarkedForDeletion = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)

        val category = intent.getStringExtra("category") ?: return

        // 🔧 CardStackView 세팅
        cardStackView = findViewById(R.id.card_stack_view)

        cardStackLayoutManager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {
                val position = cardStackLayoutManager.topPosition - 1
                if (position in photoUris.indices) {
                    val uri = photoUris[position]
                    when (direction) {
                        Direction.Left -> {
                            photosMarkedForDeletion.add(uri)
                            markPhotoForDeletion(uri)
                        }
                        Direction.Right -> {
                            // 보관: 아무 것도 안 함
                        }
                        else -> {}
                    }
                }
            }

            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: View?, position: Int) {}
            override fun onCardDisappeared(view: View?, position: Int) {}
        })

        cardStackView.layoutManager = cardStackLayoutManager
        photoCardAdapter = PhotoCardAdapter(photoUris)
        cardStackView.adapter = photoCardAdapter

        // 🔄 데이터 로딩
        lifecycleScope.launch {
            val uris = loadUrisByCategory(category)
            photoUris.clear()
            photoUris.addAll(uris)
            photoCardAdapter.notifyDataSetChanged()
        }

        // 🏠 홈 이동
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // 🗑 휴지통 이동
        findViewById<ImageButton>(R.id.btn_trash).setOnClickListener {
            val intent = Intent(this, TrashPreviewActivity::class.java)
            intent.putParcelableArrayListExtra("photo_uris", ArrayList(photosMarkedForDeletion))
            startActivity(intent)
        }
    }

    private suspend fun loadUrisByCategory(category: String): List<Uri> = withContext(Dispatchers.IO) {
        val snapshot = Firebase.firestore
            .collection("photoLabels")
            .whereEqualTo("category", category)
            .get()
            .await()
        Log.d("CategoryList", "불러온 문서 수: ${snapshot.size()}")
        snapshot.documents.mapNotNull {
            val uriStr = it.getString("uri")
            uriStr?.let { Uri.parse(it) }
        }
    }

    private fun markPhotoForDeletion(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val data = mapOf(
                    "uri" to uri.toString(),
                    "timestamp" to System.currentTimeMillis(),
                    "marked" to true
                )
                Firebase.firestore.collection("trashPhotos")
                    .document(fileName)
                    .set(data)
                    .await()
                Log.d("Firestore", "삭제 대상 등록됨: $fileName")
            } catch (e: Exception) {
                Log.e("Firestore", "삭제 등록 실패", e)
            }
        }
    }

    fun categorizeLabel(label: String, mapping: Map<String, List<String>>): String {
        val normalized = label.lowercase()
        for ((category, tagList) in mapping) {
            if (tagList.any { it.lowercase() == normalized }) {
                return category
            }
        }
        return "etc"
    }

    fun getCategoryFromVisionTags(
        visionTags: List<String>,
        mapping: Map<String, List<String>>
    ): String {
        for (tag in visionTags) {
            val category = categorizeLabel(tag, mapping)
            if (category != "etc") return category // 첫 매칭된 카테고리 우선 리턴
        }
        return "etc"
    }
}
