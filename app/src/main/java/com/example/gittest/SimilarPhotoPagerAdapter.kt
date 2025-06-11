package com.example.gittest

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class SimilarPhotoPagerAdapter(
    private val context: Context,
    private val clusters: List<List<Uri>>
) : RecyclerView.Adapter<SimilarPhotoPagerAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return PhotoViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = clusters[position].firstOrNull()
        if (uri != null) {
            holder.imageView.setImageURI(uri)
        }
    }

    override fun getItemCount(): Int = clusters.size
}
