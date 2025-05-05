package com.example.boardingscreen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager

class StartActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: ScreenSlidePageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        viewPager = findViewById(R.id.pager)

        pagerAdapter = ScreenSlidePageAdapter(supportFragmentManager)
        viewPager.setAdapter(pagerAdapter)

    }

}


