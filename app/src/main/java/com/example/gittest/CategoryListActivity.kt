package com.example.gittest

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.yuyakaido.android.cardstackview.*

class CategoryListActivity : AppCompatActivity() {

    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var photoCardAdapter: PhotoCardAdapter
    private val photoUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)

        // 1. CardStackView 세팅
        cardStackView = findViewById(R.id.card_stack_view)
        cardStackLayoutManager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {
                // Optional: 삭제 처리 로직
            }
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: android.view.View?, position: Int) {}
            override fun onCardDisappeared(view: android.view.View?, position: Int) {}
        })

        cardStackView.layoutManager = cardStackLayoutManager
        photoCardAdapter = PhotoCardAdapter(photoUris)
        cardStackView.adapter = photoCardAdapter

        // 2. 분류 정보로 사진 불러오기
        val category = intent.getStringExtra("category") ?: return

        lifecycleScope.launch {
            val uris = loadUrisByCategory(category)
            photoUris.clear()
            photoUris.addAll(uris)
            photoCardAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun loadUrisByCategory(cat: String): List<Uri> =
        withContext(Dispatchers.IO) {
            val snap = Firebase.firestore
                .collection("photoLabels")
                .whereEqualTo("category", cat)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                doc.getString("uri")?.let { Uri.parse(it) }
            }
        }
}
