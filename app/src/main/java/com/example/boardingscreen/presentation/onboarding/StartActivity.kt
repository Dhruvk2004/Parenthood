package com.example.boardingscreen.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.boardingscreen.R
import com.example.boardingscreen.presentation.auth.UserType
import com.google.firebase.auth.FirebaseAuth

class StartActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: ScreenSlidePageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is logged in, go to UserType which will handle Parent/Child routing
            startActivity(Intent(this, UserType::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_start)

        viewPager = findViewById(R.id.pager)

        pagerAdapter = ScreenSlidePageAdapter(supportFragmentManager)
        viewPager.setAdapter(pagerAdapter)

    }

}


