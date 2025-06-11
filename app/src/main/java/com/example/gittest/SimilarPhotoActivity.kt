package com.example.gittest

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private val uriToClusterMap = mutableMapOf<Uri, List<Uri>>()
    private val photoUris = mutableListOf<Uri>()

    // 각 클러스터는 유사한 사진들의 리스트
    private var similarClusters: List<List<Uri>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_same_delete)

        // 홈 버튼
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener { finish() }
        // 쓰레기통 버튼
        findViewById<ImageButton>(R.id.btn_trash).setOnClickListener {
            startActivity(Intent(this, TrashPreviewActivity::class.java))
        }

        viewPager = findViewById(R.id.view_pager)
        deleteButton = findViewById(R.id.btn_delete_same_photo)

        // 모델 초기화
        TFLiteHelper.initialize(this)
         //
        // 사진 클러스터 찾기
        lifecycleScope.launch {
            similarClusters = findSimilarPhotos()
            Log.d("ClusterDebug","총 ${similarClusters.size}개의 유사 그룹 발견")
            if (similarClusters.isNotEmpty()) {
                viewPager.adapter = SimilarPhotoPagerAdapter(this@SimilarPhotoActivity, photoUris)
            } else {
                Toast.makeText(
                    this@SimilarPhotoActivity,
                    "유사한 사진 그룹을 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                deleteButton.isEnabled = false
            }
        }

        // “한 장만 남기고 지우기” 클릭 (삭제 전 확인 다이얼로그 추가)
        deleteButton.setOnClickListener {
            val currentCluster = similarClusters.getOrNull(viewPager.currentItem).orEmpty()
            if (currentCluster.size <= 1) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("이 그룹에는 ${currentCluster.size}장의 사진이 유사하다고 인식되었습니다. 대표 사진 1장을 제외한 ${currentCluster.size - 1}장을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    val toDelete = currentCluster.drop(1)
                    deletePhotosFromGallery(toDelete)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    // SAF + direct delete 통합 함수
    private fun deletePhotosFromGallery(photoUris: List<Uri>) {
        if (photoUris.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 이상: SAF delete request
            val intentSender =
                MediaStore.createDeleteRequest(contentResolver, photoUris).intentSender
            startIntentSenderForResult(
                intentSender,
                REQUEST_DELETE,
                null, 0, 0, 0, null
            )
        } else {
            // Android 10 이하: 직접 삭제
            for (uri in photoUris) {
                contentResolver.delete(uri, null, null)
            }
            Toast.makeText(this, "${photoUris.size}장 삭제 완료", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // SAF 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DELETE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "삭제 실패 또는 취소됨", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    companion object {
        private const val REQUEST_DELETE = 1001
    }

    // 기존 findSimilarPhotos()와 loadBitmap() 수정: eps 낮춰 오탐 감소
    private suspend fun findSimilarPhotos(): List<List<Uri>> = withContext(Dispatchers.Default) {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val uris = mutableListOf<Uri>()
        val embeddings = mutableListOf<FloatArray>()

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                loadBitmap(uri)?.let { bitmap ->
                    uris.add(uri)
                    embeddings.add(TFLiteHelper.getEmbedding(bitmap))
                }
            }
        }

        // 유클리드 거리를 cosine 거리로 변경하거나, eps 값 조정
        val dbscan = SimilarityCluster(embeddings, eps = 5f, minPts = 2)
        val labels = dbscan.cluster()
        val clusters = mutableMapOf<Int, MutableList<Uri>>()
        for (i in labels.indices) {
            val label = labels[i]
            if (label != -1) clusters.getOrPut(label) { mutableListOf() }.add(uris[i])
        }

        val rawClusters = clusters.values.filter { it.size >= 2 }
        for (cluster in rawClusters) {
            for (uri in cluster) {
                uriToClusterMap[uri] = cluster
                photoUris.add(uri)
            }
        }
        return@withContext rawClusters
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
