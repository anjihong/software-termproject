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
import android.widget.TextView
import java.text.SimpleDateFormat
import androidx.exifinterface.media.ExifInterface
import java.util.Date
import java.util.Locale
import com.example.gittest.network.OpenWeatherClient


class MainActivity : AppCompatActivity() {
    private val photoUris = mutableListOf<Uri>()
    private val photosMarkedForDeletion = mutableListOf<Uri>()
    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var photoCardAdapter: PhotoCardAdapter
    private lateinit var tvLocation: TextView
    private lateinit var tvDatetime: TextView
    private lateinit var tvWeather: TextView

    private fun extractExif(uri: Uri): Triple<Double?, Double?, Long?> {
        val input = contentResolver.openInputStream(uri) ?: return Triple(null,null,null)
        val exif = androidx.exifinterface.media.ExifInterface(input)
        input.close()

        // 날짜
        val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val timestamp = dateStr?.let {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                .parse(it)?.time
        }

        // GPS
        val latLong = exif.latLong
        val lat = latLong?.get(0)
        val lon = latLong?.get(1)

        return Triple(lat, lon, timestamp)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocation = findViewById(R.id.tv_location)
        tvDatetime = findViewById(R.id.tv_datetime)
        tvWeather  = findViewById(R.id.tv_weather)

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
            override fun onCardAppeared(view: View?, position: Int) {
                // 1) 현재 카드 URI
                val uri = photoUris[position]

                // 2) EXIF에서 위치·시간 추출
                val (lat, lon, ts) = extractExif(uri)

                // 3) 날짜 표시
                tvDatetime.text = ts?.let {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Date: —"

                // 4) 위치 표시
                tvLocation.text = if (lat != null && lon != null)
                    "Location: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
                else "Location: —"

                // 5) 날씨 API 호출
                if (lat != null && lon != null) {
                    lifecycleScope.launch {
                        try {
                            val resp = OpenWeatherClient.service.getCurrentWeather(
                                lat, lon, "metric", OpenWeatherClient.API_KEY
                            )

                            Log.d("WeatherTest", "WeatherResponse for $lat,$lon → $resp")
                            val weatherMain = resp.weather.firstOrNull()?.main ?: "—"
                            tvWeather.text = "Weather: $weatherMain, ${resp.main.temp}°C"
                        } catch (e: Exception) {
                            Log.e("WeatherTest", "Weather API error", e)
                            tvWeather.text = "Weather: N/A"
                        }
                    }
                } else {
                    tvWeather.text = "Weather: —"
                }
            }

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