package com.example.gittest

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TrashPreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val photoUris = intent.getParcelableArrayListExtra<Uri>("photo_uris") ?: listOf()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter(photoUris)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btn_delete_all).setOnClickListener {
            Toast.makeText(this, "사진 일괄 삭제 처리!", Toast.LENGTH_SHORT).show()
        }
    }
}
