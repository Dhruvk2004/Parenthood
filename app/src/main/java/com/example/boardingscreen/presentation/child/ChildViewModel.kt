package com.example.boardingscreen.presentation.child

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardingscreen.data.model.Geofence_details
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChildViewModel(
    private val sharedPreferences: SharedPreferences,
    private val servicePreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<ChildUiState>()
    val uiState: LiveData<ChildUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var _isServiceRunning = false
    val isServiceRunning: Boolean get() = _isServiceRunning

    init {
        _isServiceRunning = servicePreferences.getBoolean(Constants.KEY_SERVICE_ENABLED, false)
    }

    fun loadChildProfile() {
        viewModelScope.launch {
            _uiState.value = ChildUiState.Loading

            // Try to load from SharedPreferences first
            val savedName = sharedPreferences.getString(Constants.KEY_CHILD_NAME, null)
            val savedEmail = sharedPreferences.getString(Constants.KEY_CHILD_EMAIL, null)
            val savedGender = sharedPreferences.getString(Constants.KEY_CHILD_GENDER, null)

            if (savedName != null && savedEmail != null) {
                _uiState.value = ChildUiState.ProfileLoaded(savedName, savedEmail, savedGender, _isServiceRunning)
            } else {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    _uiState.value = ChildUiState.NoInternet
                    return@launch
                }
                fetchProfileFromFirestore()
            }
        }
    }

    private suspend fun fetchProfileFromFirestore() {
        val userId = auth.uid
        if (userId == null) {
            _uiState.value = ChildUiState.Error("User not logged in")
            return
        }

        try {
            val document = db.collection(Constants.COLLECTION_CHILD)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val name = document.getString("name") ?: "Unknown"
                val email = document.getString("email") ?: "Unknown"
                val securePin = document.getString("secure_pin")
                val gender = sharedPreferences.getString(Constants.KEY_CHILD_GENDER, null)

                // Save to SharedPreferences
                sharedPreferences.edit()
                    .putString(Constants.KEY_CHILD_NAME, name)
                    .putString(Constants.KEY_CHILD_EMAIL, email)
                    .putString(Constants.KEY_SECURE_PIN, securePin)
                    .apply()

                _uiState.value = ChildUiState.ProfileLoaded(name, email, gender, _isServiceRunning)
            } else {
                _uiState.value = ChildUiState.Error("Profile not found")
            }
        } catch (e: Exception) {
            Log.e("ChildViewModel", "Error fetching profile: ${e.message}")
            _uiState.value = ChildUiState.Error("Error loading profile: ${e.message}")
        }
    }

    fun saveGender(gender: String) {
        sharedPreferences.edit()
            .putString(Constants.KEY_CHILD_GENDER, gender)
            .apply()
    }

    fun getGender(): String? {
        return sharedPreferences.getString(Constants.KEY_CHILD_GENDER, null)
    }

    fun verifyPinAndStartServices(enteredPin: String, onSuccess: () -> Unit) {
        val savedPin = sharedPreferences.getString(Constants.KEY_SECURE_PIN, null)
        if (enteredPin == savedPin) {
            _isServiceRunning = true
            saveServiceState(true)
            _uiState.value = ChildUiState.ServicesStarted
            onSuccess()
        } else {
            _uiState.value = ChildUiState.PinError("Incorrect Security Pin")
        }
    }

    fun verifyPinAndStopServices(enteredPin: String, onSuccess: () -> Unit) {
        val savedPin = sharedPreferences.getString(Constants.KEY_SECURE_PIN, null)
        if (enteredPin == savedPin) {
            _isServiceRunning = false
            saveServiceState(false)
            _uiState.value = ChildUiState.ServicesStopped
            onSuccess()
        } else {
            _uiState.value = ChildUiState.PinError("Incorrect Security Pin")
        }
    }

    fun verifyPinAndLogout(enteredPin: String, onSuccess: () -> Unit) {
        val savedPin = sharedPreferences.getString(Constants.KEY_SECURE_PIN, null)
        if (enteredPin == savedPin) {
            // Clear SharedPreferences
            sharedPreferences.edit().clear().apply()
            servicePreferences.edit().clear().apply()
            
            // Sign out from FirebaseAuth
            auth.signOut()
            
            _uiState.value = ChildUiState.LogoutSuccess
            onSuccess()
        } else {
            _uiState.value = ChildUiState.PinError("Incorrect Security Pin")
        }
    }

    fun verifyPin(enteredPin: String, onSuccess: () -> Unit) {
        val savedPin = sharedPreferences.getString(Constants.KEY_SECURE_PIN, null)
        if (enteredPin == savedPin) {
            _uiState.value = ChildUiState.PinVerified
            onSuccess()
        } else {
            _uiState.value = ChildUiState.PinError("Incorrect Security Pin")
        }
    }

    fun loadGeofences() {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = ChildUiState.Error(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            val userId = auth.uid
            if (userId == null) {
                _uiState.value = ChildUiState.Error("User not logged in")
                return@launch
            }

            try {
                val querySnapshot = db.collection(Constants.COLLECTION_CHILD)
                    .document(userId)
                    .collection(Constants.COLLECTION_GEO_DETAILS)
                    .get()
                    .await()

                val geofenceList = mutableListOf<Geofence_details>()

                for (document in querySnapshot.documents) {
                    val geoId = document.id
                    val geoName = document.getString("Name")
                    val placeName = document.getString("place")
                    val radius = document.getDouble("radius")?.toInt()

                    if (geoId != null && geoName != null && placeName != null && radius != null) {
                        geofenceList.add(Geofence_details(geoId, geoName, placeName, radius))
                    }
                }

                _uiState.value = ChildUiState.GeofencesLoaded(geofenceList)
            } catch (e: Exception) {
                Log.e("ChildViewModel", "Error loading geofences: ${e.message}")
                _uiState.value = ChildUiState.Error("Failed to retrieve geofence details: ${e.message}")
            }
        }
    }

    private fun saveServiceState(isRunning: Boolean) {
        servicePreferences.edit()
            .putBoolean(Constants.KEY_SERVICE_ENABLED, isRunning)
            .apply()
    }

    fun isServicesEnabled(): Boolean {
        return _isServiceRunning
    }
}
