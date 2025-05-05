package com.example.boardingscreen

import android.annotation.SuppressLint
import android.content.*
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
class LoginActivity : AppCompatActivity() {
    lateinit var auth:FirebaseAuth
    lateinit var email_txt:TextInputEditText
    lateinit var password_txt:TextInputEditText
    lateinit var email_layout:TextInputLayout
    lateinit var pass_layout:TextInputLayout
    private var oneTapClient: SignInClient? =null
    lateinit var signinrequest: BeginSignInRequest
    private var db= Firebase.firestore
    private var user_type:String=""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Accessing the widgets
        auth = Firebase.auth
        email_layout=findViewById(R.id.login_layout)
        pass_layout=findViewById(R.id.passwd_layout)
        email_txt=findViewById(R.id.login_email)
        password_txt=findViewById(R.id.login_pass)
        user_type= intent.getStringExtra("utype").toString()

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

    fun Email_login(view: View){
        if(checkAllField()){
            auth.signInWithEmailAndPassword(email_txt.text.toString(),password_txt.text.toString()).addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(this,"Signed In Successfully", Toast.LENGTH_LONG).show()

                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("user_type", user_type) // userType is "parent" or "child"
                    editor.apply()

                    if(user_type=="Parent"){
                        val intent=Intent(this,ParentActivity::class.java)
                        startActivity(intent)
                    }else{
                        val intent=Intent(this,AskPermissions::class.java)
                        startActivity(intent)
                    }

                }
                else{
                    Toast.makeText(this,"Invalid Username or Password", Toast.LENGTH_LONG).show()
                    Log.e("error",it.exception.toString())
                    //not signed in
                }
            }
        }
    }

    fun call_signup_page(view: View) {
        val intent=Intent(this,SignUpActivity::class.java)
        intent.putExtra("utype",user_type)
        startActivity(intent)
    }

    //Function for email and password validation
    private fun checkAllField(): Boolean {
        // Clear previous errors
        email_layout.error = null
        pass_layout.error = null

        val email = email_txt.text.toString()
        val password = password_txt.text.toString()

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

        return true
    }

    fun google_login(view: View){
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
                                Toast.makeText(this, "Signed In Successfully", Toast.LENGTH_LONG).show()
                                if(user_type=="Parent"){
                                    val intent=Intent(this,ParentActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }else{
                                    val intent=Intent(this,AskPermissions::class.java)
                                    startActivity(intent)
                                    finish()
                                }
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