package com.example.boardingscreen.presentation.parent.profile

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<ProfileUiState>()
    val uiState: LiveData<ProfileUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var childIdsList: MutableList<String> = mutableListOf()

    init {
        loadChildIdsList()
    }

    private fun loadChildIdsList() {
        val childIdsJson = sharedPreferences.getString(Constants.KEY_CHILD_IDS, null)
        childIdsList = if (childIdsJson != null) {
            Gson().fromJson(childIdsJson, Array<String>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            // Try to load from SharedPreferences first
            val savedName = sharedPreferences.getString(Constants.KEY_PARENT_NAME, null)
            val savedEmail = sharedPreferences.getString("email", null)
            val savedGender = sharedPreferences.getString("gender", null)

            if (savedName != null && savedEmail != null) {
                _uiState.value = ProfileUiState.Success(savedName, savedEmail, savedGender)
            } else {
                // Fetch from Firestore
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    _uiState.value = ProfileUiState.NoInternet
                    return@launch
                }

                fetchProfileFromFirestore()
            }
        }
    }

    private suspend fun fetchProfileFromFirestore() {
        val userId = auth.uid
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("User not logged in")
            return
        }

        try {
            val document = db.collection(Constants.COLLECTION_PARENT)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val name = document.getString("name") ?: "Unknown"
                val email = document.getString("email") ?: "Unknown"
                val gender = sharedPreferences.getString("gender", null)

                // Save to SharedPreferences
                sharedPreferences.edit()
                    .putString(Constants.KEY_PARENT_NAME, name)
                    .putString("email", email)
                    .apply()

                _uiState.value = ProfileUiState.Success(name, email, gender)
            } else {
                _uiState.value = ProfileUiState.Error("Profile not found")
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error fetching profile: ${e.message}")
            _uiState.value = ProfileUiState.Error("Error loading profile: ${e.message}")
        }
    }

    fun saveGender(gender: String) {
        sharedPreferences.edit()
            .putString("gender", gender)
            .apply()

        // Update current state with new gender
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(gender = gender)
        }
    }

    fun getGender(): String? {
        return sharedPreferences.getString("gender", null)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Clear SharedPreferences
                sharedPreferences.edit().clear().apply()

                // Sign out from FirebaseAuth
                auth.signOut()

                _uiState.value = ProfileUiState.LogoutSuccess
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during logout: ${e.message}")
                _uiState.value = ProfileUiState.Error("Logout failed: ${e.message}")
            }
        }
    }

    fun addChildDevice(scannedChildId: String) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = ProfileUiState.Error(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            val userId = auth.uid
            if (userId == null) {
                _uiState.value = ProfileUiState.Error("User not logged in")
                return@launch
            }

            // Check if already connected
            if (childIdsList.contains(scannedChildId)) {
                _uiState.value = ProfileUiState.Error("Child Profile already connected")
                return@launch
            }

            try {
                // Check if child document exists
                val childDoc = db.collection(Constants.COLLECTION_CHILD)
                    .document(scannedChildId)
                    .get()
                    .await()

                if (!childDoc.exists()) {
                    _uiState.value = ProfileUiState.Error("Invalid Child Profile")
                    return@launch
                }

                // Update parent's child_ids
                db.collection(Constants.COLLECTION_PARENT)
                    .document(userId)
                    .update("child_ids", FieldValue.arrayUnion(scannedChildId))
                    .await()

                // Update local list and SharedPreferences
                childIdsList.add(scannedChildId)
                val updatedChildIdsJson = Gson().toJson(childIdsList)
                sharedPreferences.edit()
                    .putString(Constants.KEY_CHILD_IDS, updatedChildIdsJson)
                    .apply()

                _uiState.value = ProfileUiState.ChildAdded

                // Reload profile to restore state
                loadProfile()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error adding child: ${e.message}")
                _uiState.value = ProfileUiState.Error("Error adding child: ${e.message}")
            }
        }
    }

    fun isChildAlreadyConnected(childId: String): Boolean {
        return childIdsList.contains(childId)
    }
}
