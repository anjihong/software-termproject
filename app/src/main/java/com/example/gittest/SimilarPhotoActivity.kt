package com.example.gittest

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.gittest.ml.SimilarityCluster
import com.example.gittest.ml.TFLiteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimilarPhotoActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var deleteButton: Button

    // 각 클러스터는 유사한 사진들의 리스트
    private var similarClusters: List<List<Uri>> = emptyList()
    private var currentClusterIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_same_delete)

        viewPager = findViewById(R.id.view_pager)
        deleteButton = findViewById(R.id.btn_delete_same_photo)

        TFLiteHelper.initialize(this)

        lifecycleScope.launch {
            similarClusters = findSimilarPhotos()
            if (similarClusters.isNotEmpty()) {
                viewPager.adapter = SimilarPhotoPagerAdapter(this@SimilarPhotoActivity, similarClusters)
            } else {
                Toast.makeText(this@SimilarPhotoActivity, "동일한 사진을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                deleteButton.isEnabled = false
            }
        }

        deleteButton.setOnClickListener {
            if (similarClusters.isNotEmpty()) {
                val currentCluster = similarClusters[viewPager.currentItem]
                if (currentCluster.size > 1) {
                    for (i in 1 until currentCluster.size) {
                        contentResolver.delete(currentCluster[i], null, null)
                    }
                    Toast.makeText(this, "${currentCluster.size - 1}장 삭제 완료", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private suspend fun findSimilarPhotos(): List<List<Uri>> = withContext(Dispatchers.Default) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val uris = mutableListOf<Uri>()
        val embeddings = mutableListOf<FloatArray>()

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val bitmap = loadBitmap(uri)
                if (bitmap != null) {
                    uris.add(uri)
                    embeddings.add(TFLiteHelper.getEmbedding(bitmap))
                }
            }
        }

        val dbscan = SimilarityCluster(embeddings, eps = 10f, minPts = 2)
        val labels = dbscan.cluster()

        val clusters = mutableMapOf<Int, MutableList<Uri>>()
        for (i in labels.indices) {
            val label = labels[i]
            if (label != -1) {
                clusters.getOrPut(label) { mutableListOf() }.add(uris[i])
            }
        }

        return@withContext clusters.values.filter { it.size >= 2 }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            Log.e("BitmapLoad", "오류 발생", e)
            null
        }
    }
}
