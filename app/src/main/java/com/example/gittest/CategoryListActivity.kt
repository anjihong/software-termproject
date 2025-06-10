package com.example.gittest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity   // 이 import가 꼭 필요합니다.

class CategoryListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_delete)

        // 여기서 intent.extras 로 넘어온 category 받아서 RecyclerView 세팅…
    }
}
