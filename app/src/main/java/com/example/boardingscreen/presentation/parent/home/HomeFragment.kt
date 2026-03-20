package com.example.boardingscreen.presentation.parent.home

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.*
import androidx.fragment.app.*
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.boardingscreen.R
import com.example.boardingscreen.data.model.Child
import com.example.boardingscreen.presentation.parent.home.HomeUiState
import com.example.boardingscreen.presentation.parent.home.HomeViewModel
import com.example.boardingscreen.presentation.parent.home.HomeViewModelFactory
import com.google.android.gms.common.moduleinstall.*
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.*

class HomeFragment : Fragment() {

    private var isScannerInstalled = false
    private lateinit var name_txt: TextView
    private lateinit var noChildProfileText: TextView
    private lateinit var addChildButton: AppCompatButton
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var child_list: Spinner
    private lateinit var child_card_head: AppCompatTextView
    private lateinit var child_info_head: AppCompatTextView
    private lateinit var time_card: MaterialCardView
    private lateinit var location_card: MaterialCardView
    private lateinit var app_time_limit_card: MaterialCardView
    private lateinit var card_bg: FragmentContainerView
    private lateinit var child_card: MaterialCardView
    private lateinit var noChildImage: AppCompatImageView
    private lateinit var time_value_txt: TextView
    private lateinit var place_txt: TextView
    private lateinit var limit_value_txt: TextView
    
    // Lottie views
    private lateinit var loadingContainer: FrameLayout
    private lateinit var noInternetContainer: FrameLayout
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var retryButton: AppCompatButton

    private lateinit var viewModel: HomeViewModel
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        initializeViewModel()
        initializeScanner()
        observeUiState()

        addChildButton.setOnClickListener {
            addChildDevice()
        }

        // Load data
        viewModel.loadHomeData()
    }

    private fun initializeViews(view: View) {
        noChildProfileText = view.findViewById(R.id.no_child_profile_text)
        addChildButton = view.findViewById(R.id.add_child_btn)
        noChildImage = view.findViewById(R.id.no_child_image)
        name_txt = view.findViewById(R.id.name_label)
        child_list = view.findViewById(R.id.child_id_spinner)
        child_card = view.findViewById(R.id.addchild_card)
        card_bg = view.findViewById(R.id.card_bg_id)
        child_card_head = view.findViewById(R.id.child_head)
        child_info_head = view.findViewById(R.id.child_info_head)
        time_card = view.findViewById(R.id.app_time_card)
        location_card = view.findViewById(R.id.location_card)
        app_time_limit_card = view.findViewById(R.id.total_app_limit_card)
        time_value_txt = view.findViewById(R.id.time_value_txt)
        place_txt = view.findViewById(R.id.place_txt)
        limit_value_txt = view.findViewById(R.id.total_limit_value_txt)
        
        // Lottie views
        loadingContainer = view.findViewById(R.id.loading_container)
        noInternetContainer = view.findViewById(R.id.no_internet_container)
        lottieLoading = view.findViewById(R.id.lottie_loading)
        retryButton = view.findViewById(R.id.retry_button)
        
        retryButton.setOnClickListener {
            viewModel.loadHomeData()
        }
    }

    private fun initializeViewModel() {
        sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val factory = HomeViewModelFactory(requireContext(), sharedPref)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }

    private fun initializeScanner() {
        installGoogleScanner()
        val options = intializaGoogleScanner()
        scanner = GmsBarcodeScanning.getClient(requireContext(), options)
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeUiState.Loading -> showLoadingState(state.parentName)
                is HomeUiState.NoInternet -> showNoInternetState(state.parentName)
                is HomeUiState.NoChild -> showNoChildState(state.parentName)
                is HomeUiState.Success -> showSuccessState(state)
                is HomeUiState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun showLoadingState(parentName: String?) {
        hideAllViews()
        if (!parentName.isNullOrEmpty()) {
            name_txt.visibility = View.VISIBLE
            name_txt.text = parentName
        }
        loadingContainer.visibility = View.VISIBLE
        lottieLoading.playAnimation()
    }

    private fun showNoInternetState(parentName: String?) {
        hideAllViews()
        if (!parentName.isNullOrEmpty()) {
            name_txt.visibility = View.VISIBLE
            name_txt.text = parentName
        }
        noInternetContainer.visibility = View.VISIBLE
    }

    private fun showNoChildState(parentName: String) {
        hideAllViews()
        name_txt.visibility = View.VISIBLE
        name_txt.text = parentName
        noChildProfileText.visibility = View.VISIBLE
        addChildButton.visibility = View.VISIBLE
        noChildImage.visibility = View.VISIBLE
    }

    private fun showSuccessState(state: HomeUiState.Success) {
        hideAllViews()
        
        // Show all cards
        child_card_head.visibility = View.VISIBLE
        card_bg.visibility = View.VISIBLE
        child_card.visibility = View.VISIBLE
        child_info_head.visibility = View.VISIBLE
        time_card.visibility = View.VISIBLE
        location_card.visibility = View.VISIBLE
        app_time_limit_card.visibility = View.VISIBLE

        // Set parent name
        name_txt.text = state.parentName

        // Set card data
        time_value_txt.text = state.screenTime ?: "N/A"
        place_txt.text = state.locationName ?: "No Location"
        limit_value_txt.text = "${state.appLimitCount} apps"

        // Setup spinner with children
        setupChildSpinner(state.children, state.selectedChildId)
    }

    private fun showErrorState(message: String) {
        hideAllViews()
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun hideAllViews() {
        loadingContainer.visibility = View.GONE
        noInternetContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        noChildProfileText.visibility = View.GONE
        addChildButton.visibility = View.GONE
        noChildImage.visibility = View.GONE
        child_card_head.visibility = View.GONE
        card_bg.visibility = View.GONE
        child_card.visibility = View.GONE
        child_info_head.visibility = View.GONE
        time_card.visibility = View.GONE
        location_card.visibility = View.GONE
        app_time_limit_card.visibility = View.GONE
    }

    private fun setupChildSpinner(children: List<Child>, selectedChildId: String) {
        val childNames = children.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, childNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        child_list.adapter = adapter

        // Set selected child
        val selectedIndex = children.indexOfFirst { it.id == selectedChildId }
        if (selectedIndex != -1) {
            child_list.setSelection(selectedIndex)
        }

        // Handle selection changes
        child_list.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedChild = children[position]
                viewModel.onChildSelected(selectedChild.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun intializaGoogleScanner(): GmsBarcodeScannerOptions {
        return GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }

    private fun installGoogleScanner() {
        val moduleInstall = ModuleInstall.getClient(requireContext())
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(GmsBarcodeScanning.getClient(requireContext()))
            .build()

        moduleInstall.installModules(moduleInstallRequest).addOnSuccessListener {
            isScannerInstalled = true
        }.addOnFailureListener {
            isScannerInstalled = false
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun addChildDevice() {
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
                    viewModel.scanQRCode(result)
                } else {
                    Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnCanceledListener {
            Toast.makeText(requireContext(), "Scan Cancelled", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Log.e("HomeFragment", "Scan error: ${it.message}")
            Toast.makeText(requireContext(), "Issue found during scan", Toast.LENGTH_LONG).show()
        }
    }
}