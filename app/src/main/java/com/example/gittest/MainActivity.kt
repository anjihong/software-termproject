package com.example.gittest

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.widget.Button
import androidx.navigation.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.yuyakaido.android.cardstackview.*


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var photoAdapter: PhotoAdapter
    private val photoUris = mutableListOf<Uri>()
    private val photosMarkedForDeletion = mutableListOf<Uri>()
    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var photoCardAdapter: PhotoCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestStoragePermissionIfNeeded()

        findViewById<Button>(R.id.btn_category_delete)
            .setOnClickListener {
                startActivity(
                    Intent(this, CategoryDeleteActivity::class.java)
                )
            }

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
                            // 보존 - 아무 동작 안함
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


        loadLatestImages()

        val btnTrash: ImageButton = findViewById(R.id.btn_trash)
        btnTrash.setOnClickListener {
            showDeletionPreview()
        }

    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 100)
            }
        } else {
            // Android 12 이하
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            }
        }
    }

    private fun loadLatestImages() {
        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                val uris = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sortOrder
                )
                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        uris.add(uri)
                    }
                }
                uris
            }

            photoUris.clear()
            photoUris.addAll(images)
            photoCardAdapter.notifyDataSetChanged()
        }
    }


    private fun markPhotoForDeletion(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val firestore = Firebase.firestore
                val photo = hashMapOf(
                    "uri" to uri.toString(),
                    "timestamp" to System.currentTimeMillis(),
                    "marked" to true
                )

                firestore.collection("trashPhotos")
                    .document(fileName)
                    .set(photo)
                    .addOnSuccessListener {
                        Log.d("Firestore", "삭제 대상 등록 성공: $fileName")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "삭제 대상 등록 실패", e)
                    }

            } catch (e: Exception) {
                Log.e("Firestore", "Firestore 등록 중 오류", e)
            }
        }
    }


    private fun showDeletionPreview() {
        val intent = Intent(this, TrashPreviewActivity::class.java).apply {
            putParcelableArrayListExtra("photo_uris", ArrayList(photosMarkedForDeletion))
        }
        startActivity(intent)
    }
}