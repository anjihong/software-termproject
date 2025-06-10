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


class CategoryListActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)

        recycler = findViewById(R.id.recyclerView)
        recycler.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoAdapter(listOf())
        recycler.adapter = adapter

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
