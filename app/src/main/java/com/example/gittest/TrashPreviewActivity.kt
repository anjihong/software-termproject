package com.example.gittest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TrashPreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val trashPhotoList = mutableListOf<TrashPhoto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(this)
            }
        }

        recyclerView = findViewById(R.id.rv_trash_images)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter(emptyList()) // 일단 빈 리스트로 초기화
        recyclerView.adapter = adapter

        fetchTrashPhotos()

        findViewById<Button>(R.id.btn_empty_trash).setOnClickListener {
            deletePhotosFromGallery(trashPhotoList.map { it.uri })
            deleteFromFirebase(trashPhotoList)
            Toast.makeText(
                this,
                "사진 ${trashPhotoList.size}개 일괄 삭제 처리!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun fetchTrashPhotos() {
        val firestore = Firebase.firestore
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("trashPhotos")
                    .whereEqualTo("marked", true)
                    .get()
                    .await()

                val fetched = snapshot.documents.mapNotNull {
                    val uriStr = it.getString("uri")
                    val uri = uriStr?.let { str -> Uri.parse(str) }
                    val docId = it.id
                    if (uri != null) TrashPhoto(uri, docId) else null
                }

                trashPhotoList.clear()
                trashPhotoList.addAll(fetched)

                withContext(Dispatchers.Main) {
                    adapter = PhotoAdapter(trashPhotoList.map { it.uri })
                    recyclerView.adapter = adapter
                }

                Log.d("Firestore", "불러온 삭제 후보 수: ${trashPhotoList.size}")
            } catch (e: Exception) {
                Log.e("Firestore", "삭제 후보 불러오기 실패", e)
            }
        }
    }

    fun Context.deletePhotosFromGallery(photoUris: List<Uri>) {
        for (uri in photoUris) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intentSender = MediaStore.createDeleteRequest(contentResolver, listOf(uri)).intentSender
                    (this as? Activity)?.startIntentSenderForResult(
                        intentSender,
                        999, null, 0, 0, 0, null
                    )
                    Log.d("PhotoDelete", "삭제 요청 보냄 (Android 11+): $uri")
                } else {
                    val rows = contentResolver.delete(uri, null, null)
                    if (rows > 0) {
                        Log.d("PhotoDelete", "삭제 완료: $uri")
                    } else {
                        Log.w("PhotoDelete", "삭제 실패 또는 항목 없음: $uri")
                    }
                }
            } catch (e: Exception) {
                Log.e("PhotoDelete", "사진 삭제 중 오류: $uri", e)
            }
        }
    }

    private fun deleteFromFirebase(photoList: List<TrashPhoto>) {
        val firestore = Firebase.firestore

        lifecycleScope.launch(Dispatchers.IO) {
            for (photo in photoList) {
                try {
                    firestore.collection("trashPhotos").document(photo.documentId).delete().await()
                    Log.d("Firebase", "Firestore 문서 삭제 완료: ${photo.documentId}")
                } catch (e: Exception) {
                    Log.e("Firebase", "Firestore 문서 삭제 실패: ${photo.documentId}", e)
                }
            }
        }
    }
}
