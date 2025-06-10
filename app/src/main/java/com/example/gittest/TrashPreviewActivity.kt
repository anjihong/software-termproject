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
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TrashPreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val trashPhotoUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        // 1) 뒤로가기 버튼 (btn_back) 누르면 이전 화면(MainActivity)으로 돌아가기
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 2) 홈 버튼 (btn_home) 누르면 MainActivity로 복귀 (중복 생성 방지)
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
            Intent(this, MainActivity::class.java).apply {
                // 이미 스택에 MainActivity가 있을 수도 있으니 FLAG 처리
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(this)
            }
        }

        // 3) 전달된 URI 리스트 꺼내기
        val photoUris = intent
            .getParcelableArrayListExtra<Uri>("photo_uris")
        // 널이거나 빈 리스트면 빈 ArrayList로 초기화
            ?: arrayListOf<Uri>()

        // 4) RecyclerView 세팅
        recyclerView = findViewById(R.id.rv_trash_images)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter(photoUris)
        recyclerView.adapter = adapter

        fetchTrashPhotos()

        // 5) 휴지통 비우기 버튼 누르면 토스트 (실제 삭제 로직은 여기에)
        findViewById<Button>(R.id.btn_empty_trash).setOnClickListener {
            deletePhotosFromGallery(photoUris)
            deleteFromFirebase(photoUris)
            Toast.makeText(
                this,
                "사진 ${photoUris.size}개 일괄 삭제 처리!",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: MediaStore 삭제 등 실제 로직 추가
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

                val fetchedUris = snapshot.documents.mapNotNull {
                    val uriStr = it.getString("uri")
                    uriStr?.let { uri -> Uri.parse(uri) }
                }

                trashPhotoUris.clear()
                trashPhotoUris.addAll(fetchedUris)

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
                Log.d("Firestore", "불러온 삭제 후보 수: ${trashPhotoUris.size}")
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


    private fun deleteFromFirebase(photoUris: List<Uri>) {
        val firestore = Firebase.firestore
        val storage = Firebase.storage

        lifecycleScope.launch(Dispatchers.IO) {
            for (uri in photoUris) {
                val fileName = uri.lastPathSegment ?: continue
                try {
                    // 1. Firebase Storage 삭제
                    storage.reference.child("marked_for_deletion/$fileName").delete().await()

                    // 2. Firestore 문서 삭제
                    firestore.collection("trashPhotos").document(fileName).delete().await()

                    Log.d("Firebase", "Deleted $fileName from Firebase")
                } catch (e: Exception) {
                    Log.e("Firebase", "Failed to delete $fileName", e)
                }
            }
        }
    }
}
