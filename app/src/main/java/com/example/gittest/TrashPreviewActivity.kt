package com.example.gittest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class TrashPreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter

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


    fun Context.deletePhotosFromGallery(photoUris: List<Uri>): Int {
        var deletedCount = 0

        for (uri in photoUris) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    deletedCount++
                    Log.d("PhotoDelete", "사진 삭제 성공: $uri")
                } else {
                    Log.w("PhotoDelete", "사진 삭제 실패 또는 없음: $uri")
                }
            } catch (e: SecurityException) {
                Log.e("PhotoDelete", "보안 예외 발생: $uri", e)
            } catch (e: Exception) {
                Log.e("PhotoDelete", "예기치 못한 오류: $uri", e)
            }
        }

        return deletedCount
    }


    private fun deleteFromFirebase(photoUris: List<Uri>) {
        val firestore = Firebase.firestore
        val storage = Firebase.storage

        lifecycleScope.launch(Dispatchers.IO) {
            for (uri in photoUris) {
                val fileName = uri.lastPathSegment ?: continue
                try {
                    storage.reference.child("marked_for_deletion/$fileName").delete().await()
                    firestore.collection("deletion_queue").document(fileName).delete().await()
                    Log.d("Firebase", "Deleted $fileName from Firebase")
                } catch (e: Exception) {
                    Log.e("Firebase", "Failed to delete $fileName", e)
                }
            }
        }
    }
}
