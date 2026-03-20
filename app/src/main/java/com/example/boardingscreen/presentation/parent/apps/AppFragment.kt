package com.example.boardingscreen.presentation.parent.apps

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.boardingscreen.presentation.parent.apps.AppUiState
import com.example.boardingscreen.presentation.parent.apps.AppViewModel
import com.example.boardingscreen.presentation.parent.apps.AppViewModelFactory
import com.example.boardingscreen.R
import com.example.boardingscreen.adapters.AppTimeAdapter
import com.example.boardingscreen.data.model.AppTimeDetails

class AppFragment : Fragment() {

    private lateinit var total_app: AppCompatTextView
    private lateinit var nochild_txt: TextView
    private lateinit var appList: ListView
    private lateinit var appTimeAdapter: AppTimeAdapter
    private lateinit var viewModel: AppViewModel
    
    // Loading and No Internet views
    private lateinit var loadingContainer: FrameLayout
    private lateinit var noInternetContainer: FrameLayout
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var retryButton: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        initializeViewModel()
        observeUiState()

        viewModel.loadAppLimits()
    }

    private fun initializeViews(view: View) {
        total_app = view.findViewById(R.id.total_apps_number)
        nochild_txt = view.findViewById(R.id.no_child_txt)
        appList = view.findViewById(R.id.app_list)
        
        // Loading and No Internet views
        loadingContainer = view.findViewById(R.id.loading_container)
        noInternetContainer = view.findViewById(R.id.no_internet_container)
        lottieLoading = view.findViewById(R.id.lottie_loading)
        retryButton = view.findViewById(R.id.retry_button)
        
        retryButton.setOnClickListener {
            viewModel.loadAppLimits()
        }
    }

    private fun initializeViewModel() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val factory = AppViewModelFactory(requireContext(), sharedPref)
        viewModel = ViewModelProvider(this, factory)[AppViewModel::class.java]
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AppUiState.Loading -> showLoadingState()
                is AppUiState.NoInternet -> showNoInternetState()
                is AppUiState.NoChild -> showNoChildState()
                is AppUiState.Success -> showSuccessState(state)
                is AppUiState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun showLoadingState() {
        loadingContainer.visibility = View.VISIBLE
        noInternetContainer.visibility = View.GONE
        lottieLoading.playAnimation()
        nochild_txt.visibility = View.GONE
        appList.visibility = View.GONE
        total_app.text = "Loading..."
    }

    private fun showNoInternetState() {
        loadingContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        noInternetContainer.visibility = View.VISIBLE
        nochild_txt.visibility = View.GONE
        appList.visibility = View.GONE
        total_app.text = "No connection"
    }

    private fun showNoChildState() {
        loadingContainer.visibility = View.GONE
        noInternetContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        total_app.text = "No child selected"
        nochild_txt.visibility = View.VISIBLE
        appList.visibility = View.GONE
    }

    private fun showSuccessState(state: AppUiState.Success) {
        loadingContainer.visibility = View.GONE
        noInternetContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        nochild_txt.visibility = View.GONE
        appList.visibility = View.VISIBLE
        total_app.text = "${state.installedAppsCount} Installed Apps"

        if (isAdded) {
            context?.let {
                appTimeAdapter = AppTimeAdapter(
                    it,
                    ArrayList(state.appTimeList),
                    { appTimeDetails -> showTimeSetDialog(appTimeDetails) },
                    { appTimeDetails -> removeTimeLimit(appTimeDetails) }
                )
                appList.adapter = appTimeAdapter
            }
        }
    }

    private fun showErrorState(message: String) {
        loadingContainer.visibility = View.GONE
        noInternetContainer.visibility = View.GONE
        lottieLoading.cancelAnimation()
        nochild_txt.visibility = View.VISIBLE
        nochild_txt.text = message
        appList.visibility = View.GONE
        total_app.text = "Error fetching data"
    }

    private fun removeTimeLimit(appTimeDetails: AppTimeDetails) {
        viewModel.removeAppLimit(
            appTimeDetails.appName,
            onSuccess = {
                Toast.makeText(requireContext(), "Time limit removed", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showTimeSetDialog(appTimeDetails: AppTimeDetails) {
        val dialogView = layoutInflater.inflate(R.layout.add_app_time_dialog, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val timeSeekBar = dialog.findViewById<SeekBar>(R.id.time_seekbar)
        val timeTextView = dialog.findViewById<TextView>(R.id.rad_txt)
        val saveButton = dialog.findViewById<Button>(R.id.save_btn)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_btn)

        val currentTimeLimit = appTimeDetails.timeLimitSet
        if (currentTimeLimit > 0) {
            timeSeekBar.progress = currentTimeLimit
            timeTextView.text = "Time Limit: ${currentTimeLimit} minutes"
        } else {
            timeSeekBar.progress = 10
            timeTextView.text = "Time Limit: 10 minutes"
        }

        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                timeTextView.text = "Time Limit: ${progress} minutes"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            val newTimeLimit = timeSeekBar.progress
            viewModel.addOrUpdateAppLimit(
                appTimeDetails.appName,
                newTimeLimit,
                onSuccess = {
                    Toast.makeText(requireContext(), "Time limit saved!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
