package com.example.gittest

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gittest.ml.SimilarityCluster
import com.example.gittest.ml.TFLiteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimilarPhotoActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var deleteButton: Button

    private var clusterUris: List<Uri> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similar_photo)

        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.result_text)
        deleteButton = findViewById(R.id.delete_button)

        TFLiteHelper.initialize(this)

        lifecycleScope.launch {
            val clustered = findSimilarPhotos()
            if (clustered.isNotEmpty()) {
                clusterUris = clustered.first()
                val bitmap = loadBitmap(clusterUris[0])
                imageView.setImageBitmap(bitmap)
                resultText.text = "동일한 사진을 ${clusterUris.size}장 찾았습니다."
            } else {
                resultText.text = "동일한 사진을 찾을 수 없습니다."
                deleteButton.isEnabled = false
            }
        }

        deleteButton.setOnClickListener {
            if (clusterUris.size > 1) {
                for (i in 1 until clusterUris.size) {
                    contentResolver.delete(clusterUris[i], null, null)
                }
                Toast.makeText(this, "${clusterUris.size - 1}장 삭제 완료", Toast.LENGTH_SHORT).show()
                finish()
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
