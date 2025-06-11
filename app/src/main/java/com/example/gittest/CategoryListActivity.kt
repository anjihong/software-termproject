package com.example.gittest

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.content.ContentUris
import android.provider.MediaStore
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView


class CategoryListActivity : AppCompatActivity() {
    private lateinit var photoAdapter: PhotoAdapter
    private val photoUris = mutableListOf<Uri>()
    private val photosMarkedForDeletion = mutableListOf<Uri>()
    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var photoCardAdapter: PhotoCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)


        val category = intent.getStringExtra("category") ?: return



        // 백그라운드에서 Firestore 쿼리
        lifecycleScope.launch {
            val uris = loadUrisByCategory(category)
            adapter.updateData(uris)
        }
    }

    // Firestore에서 category 필드가 일치하는 문서들의 ID(Uri)만 가져오기
    private suspend fun loadUrisByCategory(cat: String): List<Uri> =
        withContext(Dispatchers.IO) {
            val snap = Firebase.firestore
                .collection("photoLabels")
                .whereEqualTo("category", cat)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val id = doc.id.toLongOrNull() ?: return@mapNotNull null
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            }
        }
}
