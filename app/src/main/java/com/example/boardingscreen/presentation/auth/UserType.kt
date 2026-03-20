package com.example.boardingscreen.presentation.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.boardingscreen.R
import com.example.boardingscreen.presentation.child.ChildActivity
import com.example.boardingscreen.presentation.parent.ParentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class UserType : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var parent:AppCompatButton
    lateinit var child:AppCompatButton
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_type)

        FirebaseApp.initializeApp(this)
        auth= Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Check if user is Parent or Child
            checkUserTypeAndNavigate(currentUser.uid)
            return
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

    private fun checkUserTypeAndNavigate(userId: String) {
        // Check if user exists in Parent collection
        db.collection("Parent").document(userId).get()
            .addOnSuccessListener { parentDoc ->
                if (parentDoc.exists()) {
                    // User is a Parent
                    startActivity(Intent(this, ParentActivity::class.java))
                    finish()
                } else {
                    // Check if user exists in Child collection
                    db.collection("Child").document(userId).get()
                        .addOnSuccessListener { childDoc ->
                            if (childDoc.exists()) {
                                // User is a Child
                                startActivity(Intent(this, ChildActivity::class.java))
                                finish()
                            } else {
                                // User not found in either collection, sign out
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener {
                            auth.signOut()
                        }
                }
            }
            .addOnFailureListener {
                auth.signOut()
            }
    }
}