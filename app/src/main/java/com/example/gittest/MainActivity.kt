package com.example.gittest

import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var photoAdapter: PhotoAdapter
    private val photoUris = mutableListOf<Uri>()
    private val photosMarkedForDeletion = mutableListOf<Uri>()

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

        viewPager = findViewById(R.id.photo_viewpager)

        loadLatestImages()

        val btnTrash: ImageButton = findViewById(R.id.btn_trash)
        btnTrash.setOnClickListener {
            showDeletionPreview()
        }

        viewPager.setPageTransformer(ZoomOutPageTransformer())
        setupSwipeGesture()
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

    private fun setupSwipeGesture() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val uri = photoUris[position]

                if (direction == ItemTouchHelper.LEFT) {
                    photosMarkedForDeletion.add(uri)
                    uploadToFirebase(uri)
                }
                photoUris.removeAt(position)
                photoAdapter.notifyItemRemoved(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(viewPager.getChildAt(0) as RecyclerView)
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
            Log.d("PhotoLoad", "Loaded ${images.size} images")

            photoUris.clear()
            photoUris.addAll(images)
            photoAdapter = PhotoAdapter(photoUris)
            viewPager.adapter = photoAdapter
        }

    }

    private fun uploadToFirebase(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.let {
                    val fileName = "${System.currentTimeMillis()}.jpg"
                    val ref = Firebase.storage.reference.child("marked_for_deletion/$fileName")
                    ref.putStream(it).await()
                    Log.d("Firebase", "Upload complete: $fileName")
                }
            } catch (e: Exception) {
                Log.e("Firebase", "Upload error", e)
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