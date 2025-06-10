package com.example.gittest

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PhotoCardAdapter(private val photos: List<Uri>) :
    RecyclerView.Adapter<PhotoCardAdapter.PhotoCardViewHolder>() {

    inner class PhotoCardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoCardViewHolder, position: Int) {
        Glide.with(holder.itemView)
            .load(photos[position])
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photos.size
}