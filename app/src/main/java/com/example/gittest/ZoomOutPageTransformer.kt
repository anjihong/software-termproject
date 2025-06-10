package com.example.gittest

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        val scale = 0.85f.coerceAtLeast(1 - abs(position))
        view.scaleX = scale
        view.scaleY = scale
        view.alpha = 0.5f + (scale - 0.85f) / (1 - 0.85f) * (1 - 0.5f)
    }
}