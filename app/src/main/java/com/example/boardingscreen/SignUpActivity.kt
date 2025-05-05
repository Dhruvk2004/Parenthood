package com.example.boardingscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.*
import android.view.View
import android.widget.Toast
import androidx.activity.result.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.*
import com.google.firebase.Firebase
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@Suppress("DEPRECATION")
class SignUpActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var name_txt:TextInputEditText
    lateinit var email_txt: TextInputEditText
    lateinit var password_txt: TextInputEditText
    lateinit var name_layout:TextInputLayout
    lateinit var email_layout: TextInputLayout
    lateinit var pass_layout: TextInputLayout
    private var db= Firebase.firestore
    private var oneTapClient: SignInClient? =null
    lateinit var signinrequest: BeginSignInRequest
    private var user_type:String=""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //Accessing the widgets
        auth = Firebase.auth
        name_layout=findViewById(R.id.name_layout)
        email_layout=findViewById(R.id.login_layout)
        pass_layout=findViewById(R.id.passwd_layout)
        name_txt=findViewById(R.id.signup_name)
        email_txt=findViewById(R.id.signup_email)
        password_txt=findViewById(R.id.signup_pass)
        user_type= intent.getStringExtra("utype").toString()

        // Initialize Google Sign-In client
        oneTapClient = Identity.getSignInClient(this)
        signinrequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }

    fun email_signup(view: View) {
        if(checkAllField()){
            auth.createUserWithEmailAndPassword(email_txt.text.toString(),password_txt.text.toString()).addOnCompleteListener {
                if (it.isSuccessful){
                     val userMap= hashMapOf(
                        "name" to name_txt.text.toString(),
                        "email" to email_txt.text.toString(),
                        "pass" to password_txt.text.toString()
                    )

                    val userid= FirebaseAuth.getInstance().currentUser!!.uid
                    db.collection(user_type).document(userid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this,"Account Created Successfully", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener {
                           // Toast.makeText(this,"Failed to cre", Toast.LENGTH_LONG).show()
                        }

                    val intent=Intent(this,LoginActivity::class.java)
                    intent.putExtra("utype",user_type)
                    startActivity(intent)

                }
                else{
                    // Check if the failure is due to the email already being registered
                    if (it.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "This Email is already registered.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Account Creation Failed", Toast.LENGTH_LONG).show()
                        Log.e("error", it.exception.toString())
                    }
                }
            }
        }
    }

    fun call_login_page(view: View) {
        val intent= Intent(this,LoginActivity::class.java)
        intent.putExtra("utype",user_type)
        startActivity(intent)
    }

    private fun checkAllField(): Boolean {
        // Clear previous errors
        email_layout.error = null
        pass_layout.error = null
        name_layout.error = null

        val name = name_txt.text.toString()
        val email = email_txt.text.toString()
        val password = password_txt.text.toString()

        // Check if name field is empty
        if (name.isEmpty()) {
            name_layout.error = "This is a required field"
            return false
        }

        // Check if name contains numbers or special characters
        if (!name.matches(Regex("^[a-zA-Z\\s]+$"))) {
            name_layout.error = "Name cannot contain numbers or special characters"
            return false
        }

        // Check if email field is empty
        if (email.isEmpty()) {
            email_layout.error = "This is a required field"
            return false
        }

        // Check if email format is valid
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_layout.error = "Invalid email format"
            return false
        }

        // Check if password field is empty
        if (password.isEmpty()) {
            pass_layout.error = "This is a required field"
            return false
        }

        if (password_txt.length() <6){
            pass_layout.error="Password should be atleast of 6 Characters"
            return false
        }

        if (password_txt.length() >20){
            pass_layout.error="Password should not be more than 20 characters"
            return false
        }
        return true
    }

    fun google_signup(view: View){
        CoroutineScope(Dispatchers.Main).launch {
            val result = oneTapClient?.beginSignIn(signinrequest)?.await()
            val intentSenderRequest = IntentSenderRequest.Builder(result!!.pendingIntent).build()
            activityResultLauncher.launch(intentSenderRequest)
        }
    }

    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient!!.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this, "Sign In Complete", Toast.LENGTH_LONG).show()
                                showUser()
                            } else {
                                Toast.makeText(this, "Sign In Failed", Toast.LENGTH_LONG).show()
                                Log.e("error", it.exception.toString())
                            }
                        }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }
        }

    private fun showUser() {
        val user = Firebase.auth.currentUser
        user?.let {
            val name1 = it.displayName.toString().trim()
            val email = it.email.toString().trim()
            val emailVerified = it.isEmailVerified

            val userMap = hashMapOf(
                "name" to name1,
                "email" to email
            )

            val userId = FirebaseAuth.getInstance().currentUser!!.uid

            // Check if the document already exists in Firestore
            db.collection(user_type).document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Document exists, update existing data (if necessary)
                        db.collection(user_type).document(userId).update(userMap as Map<String, Any>)
                    } else {
                        // Document does not exist, create a new one
                        db.collection(user_type).document(userId).set(userMap, SetOptions.merge())
                    }
                }
        }
    }

}