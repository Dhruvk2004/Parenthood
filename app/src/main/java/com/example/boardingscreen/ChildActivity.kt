package com.example.boardingscreen

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class ChildActivity : AppCompatActivity() {
    private lateinit var profileName: AppCompatTextView
    private lateinit var profileEmail: AppCompatTextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileImage: ImageView
    private lateinit var service_options:ImageView
    private lateinit var pencil: ImageView
    private lateinit var logout: MaterialCardView
    private lateinit var showQR: MaterialCardView
    private lateinit var showGeofences: MaterialCardView
    private lateinit var revokePermission:MaterialCardView
    private lateinit var showApps: MaterialCardView
    private lateinit var ListAdapter: ListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var services_flag:Boolean=false
    private var updateLocationService: Intent?=null
    private var geofenceEvent:Intent?=null
    private lateinit var enable_text:AppCompatTextView
    private lateinit var enable_icon:ImageView
    private lateinit var disable_text:AppCompatTextView
    private lateinit var disable_icon:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child)

        services_flag = isServiceRunning()
        updateLocationService=Intent(this,UpdateLocationService::class.java)
        geofenceEvent= Intent(this,GeofenceEvent::class.java)

        profileName=findViewById(R.id.profile_name)
        profileEmail=findViewById(R.id.profile_email)
        profileImage=findViewById(R.id.profile_image)
        pencil=findViewById(R.id.pen_icon)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        logout=findViewById(R.id.Logout_card)
        showQR=findViewById(R.id.qr_card)
        showGeofences=findViewById(R.id.geo_card)
        showApps=findViewById(R.id.apps_card)
        revokePermission=findViewById(R.id.permission_card)
        service_options=findViewById(R.id.app_activity_icon)
        enable_text=findViewById(R.id.enabled_head)
        disable_text=findViewById(R.id.disable_head)
        enable_icon=findViewById(R.id.ser_tick)
        disable_icon=findViewById(R.id.dis_tick)

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        loadGenderFromPreferences()
        loadNameAndEmail()


        pencil.setOnClickListener {
            showGenderMenu(it)
        }

        service_options.setOnClickListener {
            OpenAndHandlePopMenu(it)
        }

        revokePermission.setOnClickListener {
            removePermission_askPin()
        }

        showQR.setOnClickListener {
            showProfileQR()
        }

        showGeofences.setOnClickListener {
            showGeofenceSheet()
        }

        showApps.setOnClickListener {
            showAppsSheet()
        }

        logout.setOnClickListener {
            Logout_askPin()
        }
    }

    fun saveServiceState(isServiceRunning: Boolean) {
        val sharedPreferences = getSharedPreferences("service_pref", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_service_enabled", isServiceRunning)
        editor.apply()
    }

    // To retrieve the service state from SharedPreferences
    fun isServiceRunning(): Boolean {
        val sharedPreferences = getSharedPreferences("service_pref", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_service_enabled", false) // Default to false if not found
    }


    fun OpenAndHandlePopMenu(view:View){
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.service_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.start_ser -> {
                    start_all_services()
                    true
                }
                R.id.stop_ser -> {
                    // Toast.makeText(this,"Open Dialog box with options",Toast.LENGTH_LONG).show()
                    stop_all_services()
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    fun start_all_services(){
        if (!services_flag) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.set_pin_dialog) // Reference to your dialog layout XML
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

            val pin_layout = dialog.findViewById<TextInputLayout>(R.id.pin_layout)
            val pin_info = dialog.findViewById<TextView>(R.id.info_txt)
            val head_txt = dialog.findViewById<TextView>(R.id.head_txt)
            val savebtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
            val pin_txt = dialog.findViewById<TextView>(R.id.pin_txt)

            head_txt.text = "Enter the Security Pin"
            pin_info.visibility = View.GONE
            savebtn.text = "Check and Start"
            savebtn.setOnClickListener {
                val pin = sharedPreferences.getString("secure_pin", null)
                if (pin_txt.text.toString() == pin) {
                    startService(updateLocationService)
                    startService(geofenceEvent)
                    services_flag=true
                    saveServiceState(true)
                    enable_text.visibility=View.VISIBLE
                    enable_icon.visibility=View.VISIBLE
                    disable_text.visibility=View.GONE
                    disable_icon.visibility=View.GONE
                    dialog.dismiss()
                    Toast.makeText(this,"All Services Started",Toast.LENGTH_SHORT).show()
                } else {
                    pin_layout.error = "Incorrect Security Pin"
                }
            }
            dialog.show()
        }else{
            Toast.makeText(this,"Services are already enabled",Toast.LENGTH_SHORT).show()
        }
    }

    fun stop_all_services(){
        if (services_flag) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.set_pin_dialog) // Reference to your dialog layout XML
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

            val pin_layout = dialog.findViewById<TextInputLayout>(R.id.pin_layout)
            val pin_info = dialog.findViewById<TextView>(R.id.info_txt)
            val head_txt = dialog.findViewById<TextView>(R.id.head_txt)
            val savebtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
            val pin_txt = dialog.findViewById<TextView>(R.id.pin_txt)

            head_txt.text = "Enter the Security Pin"
            pin_info.visibility = View.GONE
            savebtn.text = "Check and Stop"
            savebtn.setOnClickListener {
                val pin = sharedPreferences.getString("secure_pin", null)
                if (pin_txt.text.toString() == pin) {
                    stopService(updateLocationService)
                    stopService(geofenceEvent)
                    services_flag=false
                    saveServiceState(false)
                    enable_text.visibility=View.GONE
                    enable_icon.visibility=View.GONE
                    disable_text.visibility=View.VISIBLE
                    disable_icon.visibility=View.VISIBLE
                    dialog.dismiss()
                    Toast.makeText(this,"All Services Stopped",Toast.LENGTH_SHORT).show()
                } else {
                    pin_layout.error = "Incorrect Security Pin"
                }
            }
            dialog.show()
        }else{
            Toast.makeText(this,"Services are already Disabled",Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppsSheet(){

    }

    fun showGenderMenu(view:View){
        val popupMenu = PopupMenu(this@ChildActivity, view)
        popupMenu.menuInflater.inflate(R.menu.profile_gen, popupMenu.menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.male -> {
                    profileImage.setImageResource(R.drawable.boy)
                    saveGenderToPreferences("Male")
                    true
                }
                R.id.female -> {
                    profileImage.setImageResource(R.drawable.girl)
                    saveGenderToPreferences("Female")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun saveGenderToPreferences(gender: String) {
        val editor = sharedPreferences.edit()
        editor.putString("child_gender", gender)
        editor.apply()
    }

    fun showGeofenceSheet(){
        val userId = auth.uid
        val bottomSheetDialog = BottomSheetDialog(this)

        // Inflate the custom layout for the bottom sheet
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.show_geofence_bottom_sheet, null)

        // Find the ImageView for the QR code and set your QR code image
        val geo_list = bottomSheetView.findViewById<ListView>(R.id.geofence_listview)

        val geofenceList= arrayListOf<Geofence_details>()
        // Reference to the 'geo_details' subcollection inside the child's document
        // Use childId to retrieve geofence details
        db.collection("Child").document(userId!!)
            .collection("geo_details")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val geo_name = document.getString("Name")
                        val placeName = document.getString("place")
                        val radius = document.getDouble("radius")?.toInt()
                        val geo_id = document.id


                        if(geo_id != null && geo_name != null && radius != null && placeName != null){
                            val geofenceDetail = Geofence_details(geo_id, geo_name, placeName, radius)
                            geofenceList.add(geofenceDetail)
                        }
                    }

                    // Set up the adapter
                    ListAdapter = ListAdapter(this, geofenceList, false) { geofence ->
                       // deleteGeofence(geofence, geofenceList,ListAdapter)
                    }
                    geo_list.adapter = ListAdapter
                } else {
                    Toast.makeText(this, "No geofence places defined", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to retrieve geofence details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        // Show the bottom sheet
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    fun showProfileQR(){
        val bottomSheetDialog = BottomSheetDialog(this)

        // Inflate the custom layout for the bottom sheet
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.qr_bottom_sheet, null)

        // Find the ImageView for the QR code and set your QR code image
        val qr_code = bottomSheetView.findViewById<ImageView>(R.id.qrcode)

        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(auth.uid, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            qr_code.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        // Set the custom view to the BottomSheetDialog
        bottomSheetDialog.setContentView(bottomSheetView)

        // Show the BottomSheetDialog
        bottomSheetDialog.show()
    }

    fun Logout_askPin(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.set_pin_dialog) // Reference to your dialog layout XML
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        val pin_layout=dialog.findViewById<TextInputLayout>(R.id.pin_layout)
        val pin_info=dialog.findViewById<TextView>(R.id.info_txt)
        val head_txt=dialog.findViewById<TextView>(R.id.head_txt)
        val savebtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
        val pin_txt = dialog.findViewById<TextView>(R.id.pin_txt)

        head_txt.text="Enter the Security Pin"
        pin_info.visibility= View.GONE
        savebtn.text="Check and Logout"
        savebtn.setOnClickListener {
            val pin=sharedPreferences.getString("secure_pin",null)
            if (pin_txt.text.toString()==pin){
                stopService(updateLocationService)
                stopService(geofenceEvent)
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()
                auth.signOut()
                dialog.dismiss()
                // Redirect to login screen (assuming you have a LoginActivity)
                val intent = Intent(this, UserType::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }else{
                pin_layout.error="Incorrect Security Pin"
            }
        }
        dialog.show()
    }

    fun removePermission_askPin(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.set_pin_dialog) // Reference to your dialog layout XML
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        val pin_layout=dialog.findViewById<TextInputLayout>(R.id.pin_layout)
        val pin_info=dialog.findViewById<TextView>(R.id.info_txt)
        val head_txt=dialog.findViewById<TextView>(R.id.head_txt)
        val savebtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
        val pin_txt = dialog.findViewById<TextView>(R.id.pin_txt)

        head_txt.text="Enter the Security Pin"
        pin_info.visibility= View.GONE
        savebtn.text="Check and Disable Permissions"
        savebtn.setOnClickListener {
            val pin=sharedPreferences.getString("secure_pin",null)
            if (pin_txt.text.toString()==pin){
                //Remove all permissions
                dialog.dismiss()
                Toast.makeText(this,"All Permissions revoked",Toast.LENGTH_SHORT).show()

            }else{
                pin_layout.error="Incorrect Security Pin"
            }
        }
        dialog.show()
    }

    fun loadGenderFromPreferences(){
        val gender = sharedPreferences.getString("child_gender", null)

        when (gender) {
            "Male" -> profileImage.setImageResource(R.drawable.boy)
            "Female" -> profileImage.setImageResource(R.drawable.girl)
            else -> profileImage.setImageResource(R.drawable.boy)
        }
    }

    fun loadNameAndEmail(){
        val savedName = sharedPreferences.getString("child_name", null)
        val savedEmail = sharedPreferences.getString("child_email", null)

        // Check if name and email are already stored in SharedPreferences
        if (savedName != null && savedEmail != null) {
            // If stored, display them in the TextViews
            profileName.text = savedName
            profileEmail.text = savedEmail
        } else {
            // If not stored, fetch from Firestore and update the UI
            fetchNameAndEmailFromFirestore()
        }
    }

    private fun fetchNameAndEmailFromFirestore() {
        val userId = auth.uid
        if (userId != null) {
            // Fetch the parent document from Firestore
            db.collection("Child").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")
                        val secure_pin=document.getString("secure_pin")
                        if (name != null && email != null && secure_pin != null) {
                            profileName.text = name
                            profileEmail.text = email
                            saveNameAndEmailToSharedPreferences(name, email, secure_pin)
                        }
                    }
                }
                .addOnFailureListener { e ->
                }
        }
    }

    private fun saveNameAndEmailToSharedPreferences(name: String, email: String, pin:String) {
        val editor = sharedPreferences.edit()
        editor.putString("child_name", name)
        editor.putString("child_email", email)
        editor.putString("secure_pin",pin)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        services_flag = isServiceRunning()

        // Update the UI based on the service state
        if (services_flag) {
            // Show that service is running

        } else {
            // Show that service is stopped

        }
    }
}