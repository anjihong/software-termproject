package com.example.gittest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class CategoryDeleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_delete)

        // 1) 홈 버튼: 이전(MainActivity)으로 돌아가기
        findViewById<ImageButton>(R.id.btn_home)
            .setOnClickListener { finish() }

        // 2) 휴지통 버튼: TrashPreviewActivity 로 이동
        findViewById<ImageButton>(R.id.btn_trash)
            .setOnClickListener {
                startActivity(
                    Intent(this, TrashPreviewActivity::class.java)
                        .apply {
                            // 필요하다면 putParcelableArrayListExtra(...) 로 URI 리스트 전달
                        }
                )
            }

        // 3) 카테고리별 리스트 화면으로 이동
//        findViewById<View>(R.id.category_landscape)
//            .setOnClickListener { startCategoryList("풍경") }
//        findViewById<View>(R.id.category_person)
//            .setOnClickListener { startCategoryList("사람") }
//        findViewById<View>(R.id.category_animal)
//            .setOnClickListener { startCategoryList("동물") }
//        findViewById<View>(R.id.category_food)
//            .setOnClickListener { startCategoryList("음식") }
//        findViewById<View>(R.id.category_etc)
//            .setOnClickListener { startCategoryList("기타") }
    }

    private fun startCategoryList(category: String) {
        Intent(this, CategoryListActivity::class.java).also {
            it.putExtra("category", category)
            startActivity(it)
        }
    }
}
