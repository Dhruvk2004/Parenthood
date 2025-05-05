package com.example.boardingscreen

import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Suppress("DEPRECATION")
class AskPermissions : AppCompatActivity(){
    lateinit var grant_per: Button
    private var db= Firebase.firestore
    lateinit var auth: FirebaseAuth
    private var onceStart=false
    // Flags for tracking permissions
    private var isLocationPermissionGranted = false
    private var isAccessibilityPermissionGranted = false
    private var isUsageAccessPermissionGranted = false

    private val backgroundLocation=registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){

        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        when{
            it.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION,false)->{

                //Get background location permission
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q){
                    if(ActivityCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )!= PackageManager.PERMISSION_GRANTED)
                    {
                        backgroundLocation.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else{
                        isLocationPermissionGranted = true
                        checkAccessibilityPermission()
                    }
                }else{
                    isLocationPermissionGranted = true
                    checkAccessibilityPermission()
                }
            }
            it.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION,false)->{
                isLocationPermissionGranted = true
                checkAccessibilityPermission()
            }
            else ->{
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_permissions)

        grant_per=findViewById(R.id.per_bt)

        auth=Firebase.auth
        grant_per.setOnClickListener {
            //Show dialog with checkbox and handle the functionality
            ShowPermissionDialog()
        }
    }

    fun ShowPermissionDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.permission_dialog) // Reference to your dialog layout XML
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        // Find views and set click listeners in dialog
        val pairButton = dialog.findViewById<AppCompatButton>(R.id.allow_btn)
        val check = dialog.findViewById<CheckBox>(R.id.permission_check)
        pairButton.setOnClickListener {
            if (check.isChecked){
                AskForPermissions()
                dialog.dismiss()
            }else{
                Toast.makeText(this,"Check the Checkbox!",Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    fun AskForPermissions(){
        onceStart=true
        checkLocationPermission()
    }

    fun checkLocationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ){
                locationPermissions.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }else{
                isLocationPermissionGranted=true
                Toast.makeText(this,"Location Permission Granted",Toast.LENGTH_SHORT).show()
                checkAccessibilityPermission()
            }
        }
    }

    fun checkAccessibilityPermission(){
        if(isAccessibilityPermissionGranted==true){
            Toast.makeText(this,"Service Permission Granted",Toast.LENGTH_SHORT).show()
            checkUsageStatsPermission()
        }else {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            isAccessibilityPermissionGranted = true
            checkUsageStatsPermission()
        }
    }

    fun checkUsageStatsPermission(){
            // Check if usage access permission is granted
            if (isUsageAccessGranted()) {
                // Usage access is granted, proceed with the next steps
                Toast.makeText(this, "Usage access permission is already granted.", Toast.LENGTH_SHORT).show()
                isUsageAccessPermissionGranted=true
                finalPermissionCheck()
            } else {
                // Usage access is not granted, prompt the user to enable it
                Toast.makeText(this, "Please enable usage access permission.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }

    // Helper function to check if usage access permission is granted
    private fun isUsageAccessGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun finalPermissionCheck(){
        if (isLocationPermissionGranted==true && isAccessibilityPermissionGranted==true && isUsageAccessPermissionGranted == true ){
            Toast.makeText(this,"All Permissions Granted",Toast.LENGTH_SHORT).show()
            //intent to child activity
            showSetPinDialog()
        }
        else{
            Toast.makeText(this,"Permissions are not allowed",Toast.LENGTH_SHORT).show()
        }
    }

    fun showSetPinDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.set_pin_dialog) // Reference to your dialog layout XML
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        val pin_layout=dialog.findViewById<TextInputLayout>(R.id.pin_layout)
        val savebtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
        val pin_txt = dialog.findViewById<TextView>(R.id.pin_txt)
        savebtn.setOnClickListener {
            if (pin_txt.text.length >= 4){
                savePinToFirestore(pin_txt.text.toString())
                dialog.dismiss()
            }else{
                pin_layout.error="Pin should be atleast of 4 Characters"
            }
        }
        dialog.show()
    }

    fun savePinToFirestore(pin: String) {
        // Get the current user ID from Firebase Authentication
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            // Create a reference to the user's document in Firestore
            val userDocRef = db.collection("Child").document(uid)

            // Store the pin in the 'secure_pin' field in the user's document
            userDocRef.update("secure_pin", pin)
                .addOnSuccessListener {
                    // Pin saved successfully, show a toast
                    Toast.makeText(this, "Pin saved successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ChildActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    // Handle failure, show an error toast
                    Toast.makeText(this, "Failed to save pin: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // No user is logged in, show an error message
            Toast.makeText(this, "No logged-in user found!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (onceStart==true){
            if(isLocationPermissionGranted==false){
                checkLocationPermission()
            }
            else if(isAccessibilityPermissionGranted==false){
                checkAccessibilityPermission()
            }
            else if(isUsageAccessPermissionGranted==false){
                checkUsageStatsPermission()
            }
        }
    }

}