package com.example.boardingscreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation

class ParentActivity : AppCompatActivity() {

    private lateinit var serviceIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_parent)

      //  serviceIntent = Intent(this, MonitoringService::class.java)
       // startService(serviceIntent)

        val bottomNavigation = findViewById<CurvedBottomNavigation>(R.id.bottomNavigation)
        bottomNavigation.add(
            CurvedBottomNavigation.Model(1,"Home",R.drawable.home_24)
        )
        bottomNavigation.add(
            CurvedBottomNavigation.Model(2,"Location",R.drawable.marker_24)
        )
        bottomNavigation.add(
            CurvedBottomNavigation.Model(3,"Apps",R.drawable.apps_24)
        )
        bottomNavigation.add(
            CurvedBottomNavigation.Model(4,"Profile",R.drawable.user_24)
        )

        bottomNavigation.setOnClickMenuListener {
            when(it.id){
                1 -> {
                    replaceFragment(HomeFragment())
                }
                2 -> {
                    replaceFragment(MapFragment())
                }
                3 -> {
                    replaceFragment(AppFragment())
                }
                4 -> {
                    replaceFragment(ProfileFragment())
                }
            }
        }
        replaceFragment(HomeFragment())
        bottomNavigation.show(1)

    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentcontainer,fragment)
            .commit()

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}