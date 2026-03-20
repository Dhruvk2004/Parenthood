package com.example.boardingscreen.presentation.parent.profile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.boardingscreen.R
import com.example.boardingscreen.presentation.auth.UserType
import com.example.boardingscreen.presentation.parent.profile.ProfileUiState
import com.example.boardingscreen.presentation.parent.profile.ProfileViewModel
import com.example.boardingscreen.presentation.parent.profile.ProfileViewModelFactory
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class ProfileFragment : Fragment() {

    private lateinit var profileName: AppCompatTextView
    private lateinit var profileEmail: AppCompatTextView
    private lateinit var profileImage: ImageView
    private lateinit var pencil: ImageView
    private lateinit var logout: MaterialCardView
    private lateinit var addchild: MaterialCardView
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: ProfileViewModel
    
    // Loading/No Internet views
    private lateinit var loadingContainer: FrameLayout
    private lateinit var noInternetContainer: FrameLayout
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var retryButton: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        initializeViewModel()
        initializeScanner()
        observeUiState()

        pencil.setOnClickListener { showPopupMenu(it) }
        logout.setOnClickListener { showLogoutDialog() }
        addchild.setOnClickListener { showAddChildDialog() }

        viewModel.loadProfile()
    }

    private fun initializeViews(view: View) {
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        logout = view.findViewById(R.id.Logout_card)
        addchild = view.findViewById(R.id.addchild_card)
        profileImage = view.findViewById(R.id.profile_image)
        pencil = view.findViewById(R.id.pen_icon)
        
        // Loading/No Internet views
        loadingContainer = view.findViewById(R.id.loading_container)
        noInternetContainer = view.findViewById(R.id.no_internet_container)
        lottieLoading = view.findViewById(R.id.lottie_loading)
        retryButton = view.findViewById(R.id.retry_button)
        
        retryButton.setOnClickListener {
            viewModel.loadProfile()
        }
    }

    private fun initializeViewModel() {
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val factory = ProfileViewModelFactory(requireContext(), sharedPreferences)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    private fun initializeScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        scanner = GmsBarcodeScanning.getClient(requireContext(), options)
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileUiState.Loading -> showLoadingState()
                is ProfileUiState.NoInternet -> showNoInternetState()
                is ProfileUiState.Success -> showSuccessState(state)
                is ProfileUiState.LogoutSuccess -> handleLogoutSuccess()
                is ProfileUiState.ChildAdded -> handleChildAdded()
                is ProfileUiState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun showLoadingState() {
        loadingContainer.visibility = View.VISIBLE
        noInternetContainer.visibility = View.GONE
        lottieLoading.playAnimation()
        profileName.text = ""
        profileEmail.text = ""
    }

    private fun showNoInternetState() {
        loadingContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        noInternetContainer.visibility = View.VISIBLE
    }

    private fun showSuccessState(state: ProfileUiState.Success) {
        loadingContainer.visibility = View.GONE
        noInternetContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        profileName.text = state.name
        profileEmail.text = state.email
        updateProfileImage(state.gender)
    }

    private fun updateProfileImage(gender: String?) {
        when (gender) {
            "Male" -> profileImage.setImageResource(R.drawable.man)
            "Female" -> profileImage.setImageResource(R.drawable.woman)
            else -> profileImage.setImageResource(R.drawable.man)
        }
    }

    private fun handleLogoutSuccess() {
        val intent = Intent(requireContext(), UserType::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun handleChildAdded() {
        Toast.makeText(requireContext(), "Child Profile added successfully", Toast.LENGTH_LONG).show()
    }

    private fun showErrorState(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profile_gen, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.male -> {
                    profileImage.setImageResource(R.drawable.man)
                    viewModel.saveGender("Male")
                    true
                }
                R.id.female -> {
                    profileImage.setImageResource(R.drawable.woman)
                    viewModel.saveGender("Female")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.logout_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val pairButton = dialog.findViewById<AppCompatButton>(R.id.yes_btn)
        val cancelbtn = dialog.findViewById<AppCompatButton>(R.id.cancel_btn)
        
        pairButton.setOnClickListener {
            viewModel.logout()
            dialog.dismiss()
        }
        cancelbtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddChildDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.scan_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val pairButton = dialog.findViewById<AppCompatButton>(R.id.pair_btn)
        pairButton.setOnClickListener {
            startScanning()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun startScanning() {
        scanner.startScan().addOnSuccessListener { scanResult ->
            val rawValue = scanResult.rawValue
            rawValue?.let { result ->
                if (!result.contains("http") && !result.contains("//")) {
                    viewModel.addChildDevice(result)
                } else {
                    Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnCanceledListener {
            Toast.makeText(requireContext(), "Scan Cancelled", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Log.d("ProfileFragment", it.toString())
            Toast.makeText(requireContext(), "Issue found during scan", Toast.LENGTH_LONG).show()
        }
    }
}