package com.example.boardingscreen.presentation.child

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
import androidx.lifecycle.ViewModelProvider
import com.example.boardingscreen.presentation.child.ChildUiState
import com.example.boardingscreen.presentation.child.ChildViewModel
import com.example.boardingscreen.presentation.child.ChildViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.firebase.auth.FirebaseAuth
import com.example.boardingscreen.R
import com.example.boardingscreen.adapters.GeofenceListAdapter
import com.example.boardingscreen.data.model.Geofence_details
import com.example.boardingscreen.services.UpdateLocationService
import com.example.boardingscreen.services.GeofenceEvent
import com.example.boardingscreen.presentation.auth.LoginActivity
import com.example.boardingscreen.presentation.auth.UserType

class ChildActivity : AppCompatActivity() {
    private lateinit var profileName: AppCompatTextView
    private lateinit var profileEmail: AppCompatTextView
    private lateinit var profileImage: ImageView
    private lateinit var service_options: ImageView
    private lateinit var pencil: ImageView
    private lateinit var logout: MaterialCardView
    private lateinit var showQR: MaterialCardView
    private lateinit var showGeofences: MaterialCardView
    private lateinit var revokePermission: MaterialCardView
    private lateinit var showApps: MaterialCardView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var servicePreferences: SharedPreferences
    private lateinit var enable_text: AppCompatTextView
    private lateinit var enable_icon: ImageView
    private lateinit var disable_text: AppCompatTextView
    private lateinit var disable_icon: ImageView

    private lateinit var viewModel: ChildViewModel
    private var updateLocationService: Intent? = null
    private var geofenceEvent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child)

        initializeViews()
        initializeViewModel()
        initializeServices()
        observeUiState()
        setupClickListeners()

        viewModel.loadChildProfile()
    }

    private fun initializeViews() {
        profileName = findViewById(R.id.profile_name)
        profileEmail = findViewById(R.id.profile_email)
        profileImage = findViewById(R.id.profile_image)
        pencil = findViewById(R.id.pen_icon)
        logout = findViewById(R.id.Logout_card)
        showQR = findViewById(R.id.qr_card)
        showGeofences = findViewById(R.id.geo_card)
        showApps = findViewById(R.id.apps_card)
        revokePermission = findViewById(R.id.permission_card)
        service_options = findViewById(R.id.app_activity_icon)
        enable_text = findViewById(R.id.enabled_head)
        disable_text = findViewById(R.id.disable_head)
        enable_icon = findViewById(R.id.ser_tick)
        disable_icon = findViewById(R.id.dis_tick)
    }

    private fun initializeViewModel() {
        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        servicePreferences = getSharedPreferences("service_pref", Context.MODE_PRIVATE)
        val factory = ChildViewModelFactory(this, sharedPreferences, servicePreferences)
        viewModel = ViewModelProvider(this, factory)[ChildViewModel::class.java]
    }

    private fun initializeServices() {
        updateLocationService = Intent(this, UpdateLocationService::class.java)
        geofenceEvent = Intent(this, GeofenceEvent::class.java)
    }

    private fun setupClickListeners() {
        pencil.setOnClickListener { showGenderMenu(it) }
        service_options.setOnClickListener { OpenAndHandlePopMenu(it) }
        revokePermission.setOnClickListener { removePermission_askPin() }
        showQR.setOnClickListener { showProfileQR() }
        showGeofences.setOnClickListener { viewModel.loadGeofences() }
        showApps.setOnClickListener { showAppsSheet() }
        logout.setOnClickListener { Logout_askPin() }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is ChildUiState.Loading -> showLoadingState()
                is ChildUiState.NoInternet -> showNoInternetState()
                is ChildUiState.ProfileLoaded -> showProfileState(state)
                is ChildUiState.GeofencesLoaded -> showGeofenceSheet(state.geofences)
                is ChildUiState.ServicesStarted -> handleServicesStarted()
                is ChildUiState.ServicesStopped -> handleServicesStopped()
                is ChildUiState.LogoutSuccess -> handleLogoutSuccess()
                is ChildUiState.PinVerified -> { /* Handled in callback */ }
                is ChildUiState.PinError -> { /* Handled in dialog */ }
                is ChildUiState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun showLoadingState() {
        profileName.text = "Loading..."
        profileEmail.text = ""
    }

    private fun showNoInternetState() {
        Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
    }

    private fun showProfileState(state: ChildUiState.ProfileLoaded) {
        profileName.text = state.name
        profileEmail.text = state.email
        updateProfileImage(state.gender)
        updateServiceUI(state.isServiceRunning)
    }

    private fun updateProfileImage(gender: String?) {
        when (gender) {
            "Male" -> profileImage.setImageResource(R.drawable.boy)
            "Female" -> profileImage.setImageResource(R.drawable.girl)
            else -> profileImage.setImageResource(R.drawable.boy)
        }
    }

    private fun updateServiceUI(isRunning: Boolean) {
        if (isRunning) {
            enable_text.visibility = View.VISIBLE
            enable_icon.visibility = View.VISIBLE
            disable_text.visibility = View.GONE
            disable_icon.visibility = View.GONE
        } else {
            enable_text.visibility = View.GONE
            enable_icon.visibility = View.GONE
            disable_text.visibility = View.VISIBLE
            disable_icon.visibility = View.VISIBLE
        }
    }

    private fun handleServicesStarted() {
        updateServiceUI(true)
        Toast.makeText(this, "All Services Started", Toast.LENGTH_SHORT).show()
    }

    private fun handleServicesStopped() {
        updateServiceUI(false)
        Toast.makeText(this, "All Services Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun handleLogoutSuccess() {
        val intent = Intent(this, UserType::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showErrorState(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun OpenAndHandlePopMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.service_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.start_ser -> {
                    start_all_services()
                    true
                }
                R.id.stop_ser -> {
                    stop_all_services()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun start_all_services() {
        if (viewModel.isServicesEnabled()) {
            Toast.makeText(this, "Services are already enabled", Toast.LENGTH_SHORT).show()
            return
        }

        showPinDialog("Enter the Security Pin", "Check and Start") { pin, dialog, pinLayout ->
            viewModel.verifyPinAndStartServices(pin) {
                startService(updateLocationService)
                startService(geofenceEvent)
                dialog.dismiss()
            }
        }
    }

    private fun stop_all_services() {
        if (!viewModel.isServicesEnabled()) {
            Toast.makeText(this, "Services are already Disabled", Toast.LENGTH_SHORT).show()
            return
        }

        showPinDialog("Enter the Security Pin", "Check and Stop") { pin, dialog, pinLayout ->
            viewModel.verifyPinAndStopServices(pin) {
                stopService(updateLocationService)
                stopService(geofenceEvent)
                dialog.dismiss()
            }
        }
    }

    private fun showPinDialog(title: String, buttonText: String, onSave: (String, Dialog, TextInputLayout) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.set_pin_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val pinLayout = dialog.findViewById<TextInputLayout>(R.id.pin_layout)
        val pinInfo = dialog.findViewById<TextView>(R.id.info_txt)
        val headTxt = dialog.findViewById<TextView>(R.id.head_txt)
        val saveBtn = dialog.findViewById<AppCompatButton>(R.id.save_btn)
        val pinTxt = dialog.findViewById<TextView>(R.id.pin_txt)

        headTxt.text = title
        pinInfo.visibility = View.GONE
        saveBtn.text = buttonText

        saveBtn.setOnClickListener {
            onSave(pinTxt.text.toString(), dialog, pinLayout)
        }

        dialog.show()
    }

    private fun showAppsSheet() {
        // TODO: Implement apps sheet
    }

    private fun showGenderMenu(view: View) {
        val popupMenu = PopupMenu(this@ChildActivity, view)
        popupMenu.menuInflater.inflate(R.menu.profile_gen, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.male -> {
                    profileImage.setImageResource(R.drawable.boy)
                    viewModel.saveGender("Male")
                    true
                }
                R.id.female -> {
                    profileImage.setImageResource(R.drawable.girl)
                    viewModel.saveGender("Female")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showGeofenceSheet(geofenceList: List<Geofence_details>) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.show_geofence_bottom_sheet, null)
        val geoList = bottomSheetView.findViewById<ListView>(R.id.geofence_listview)

        if (geofenceList.isEmpty()) {
            Toast.makeText(this, "No geofence places defined", Toast.LENGTH_SHORT).show()
        } else {
            val listAdapter = GeofenceListAdapter(this, ArrayList(geofenceList), false) { _ -> }
            geoList.adapter = listAdapter
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun showProfileQR() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.qr_bottom_sheet, null)
        val qrCode = bottomSheetView.findViewById<ImageView>(R.id.qrcode)

        val auth = FirebaseAuth.getInstance()
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
            qrCode.setImageBitmap(bmp)
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun Logout_askPin() {
        showPinDialog("Enter the Security Pin", "Check and Logout") { pin, dialog, pinLayout ->
            viewModel.verifyPinAndLogout(pin) {
                stopService(updateLocationService)
                stopService(geofenceEvent)
                dialog.dismiss()
            }
        }
    }

    private fun removePermission_askPin() {
        showPinDialog("Enter the Security Pin", "Check and Disable Permissions") { pin, dialog, pinLayout ->
            viewModel.verifyPin(pin) {
                dialog.dismiss()
                Toast.makeText(this, "All Permissions revoked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceUI(viewModel.isServicesEnabled())
    }
}
