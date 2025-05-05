package com.example.boardingscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserType : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var parent:AppCompatButton
    lateinit var child:AppCompatButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_type)

        FirebaseApp.initializeApp(this)
        auth= Firebase.auth

       val currentUser=auth.currentUser
        if(currentUser!=null) run {
            val intent = Intent(this, ParentActivity::class.java)
            startActivity(intent)
            finish()
        }

        parent=findViewById(R.id.parent_btn)
        child=findViewById(R.id.child_btn)

        parent.setOnClickListener {
            val intent= Intent(this,LoginActivity::class.java)
            intent.putExtra("utype","Parent")
            startActivity(intent)
        }

        child.setOnClickListener {
            val intent= Intent(this,LoginActivity::class.java)
            intent.putExtra("utype","Child")
            startActivity(intent)
        }
    }

}