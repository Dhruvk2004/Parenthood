package com.example.boardingscreen.presentation.parent.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardingscreen.BuildConfig
import com.example.boardingscreen.data.model.Child
import com.example.boardingscreen.data.repository.ChildRepository
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.NetworkUtils
import com.example.boardingscreen.utils.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val childRepository: ChildRepository,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<HomeUiState>()
    val uiState: LiveData<HomeUiState> = _uiState

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun loadHomeData() {
        viewModelScope.launch {
            // Get saved parent name immediately from SharedPreferences
            val savedParentName = sharedPreferences.getString(Constants.KEY_PARENT_NAME, null)
            _uiState.value = HomeUiState.Loading(savedParentName)

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = HomeUiState.Error(Constants.ERROR_AUTH_FAILED)
                return@launch
            }

            // Check network connectivity
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = HomeUiState.NoInternet(savedParentName)
                return@launch
            }

            // Load parent name
            val parentName = loadParentName(userId)

            // Load children data
            loadChildData(userId, parentName)
        }
    }

    private suspend fun loadParentName(userId: String): String {
        // Check SharedPreferences first
        val savedName = sharedPreferences.getString(Constants.KEY_PARENT_NAME, null)
        if (savedName != null) {
            return savedName
        }

        // Fetch from Firestore
        return try {
            val document = db.collection(Constants.COLLECTION_PARENT)
                .document(userId)
                .get()
                .await()

            val parentName = document.getString("name") ?: "Parent"
            
            // Store in SharedPreferences
            sharedPreferences.edit()
                .putString(Constants.KEY_PARENT_NAME, parentName)
                .apply()

            parentName
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error fetching parent name: ${e.message}")
            "Parent"
        }
    }

    private suspend fun loadChildData(userId: String, parentName: String) {
        // Check SharedPreferences for child_ids
        val storedChildIdsJson = sharedPreferences.getString(Constants.KEY_CHILD_IDS, null)
        val storedChildId = sharedPreferences.getString(Constants.KEY_CHILD_ID, null)

        if (!storedChildIdsJson.isNullOrEmpty()) {
            // Load from SharedPreferences
            val childIds = Gson().fromJson(storedChildIdsJson, Array<String>::class.java).toList()
            val selectedChildId = storedChildId ?: childIds[0]
            
            loadChildrenDetails(childIds, selectedChildId, parentName)
        } else {
            // Fetch from Firestore
            when (val result = childRepository.getChildrenForParent(userId)) {
                is Result.Success -> {
                    val children = result.data
                    if (children.isEmpty()) {
                        _uiState.value = HomeUiState.NoChild(parentName)
                    } else {
                        val childIds = children.map { it.id }
                        val selectedChildId = children[0].id
                        
                        // Store in SharedPreferences
                        storeChildIdsInSharedPrefs(selectedChildId, childIds)
                        
                        loadChildrenDetails(childIds, selectedChildId, parentName)
                    }
                }
                is Result.Error -> {
                    _uiState.value = HomeUiState.Error(result.exception.message ?: Constants.ERROR_UNKNOWN)
                }
                is Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private suspend fun loadChildrenDetails(childIds: List<String>, selectedChildId: String, parentName: String) {
        val children = mutableListOf<Child>()
        
        // Fetch all children details
        for (childId in childIds) {
            when (val result = childRepository.getChild(childId)) {
                is Result.Success -> {
                    children.add(result.data)
                }
                is Result.Error -> {
                    Log.e("HomeViewModel", "Error fetching child $childId: ${result.exception.message}")
                }
                is Result.Loading -> {}
            }
        }

        // Load card data for selected child
        loadCardData(selectedChildId, parentName, children)
    }

    private suspend fun loadCardData(childId: String, parentName: String, children: List<Child>) {
        try {
            val childDoc = db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .get()
                .await()

            val screenTime = childDoc.getString("screen_time")
            val geoPoint = childDoc.getGeoPoint("location")
            
            // Reverse geocode location
            val locationName = if (geoPoint != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        val geoApiContext = GeoApiContext.Builder()
                            .apiKey(BuildConfig.MAPS_API_KEY)
                            .build()
                        
                        val results = GeocodingApi.reverseGeocode(geoApiContext, latLng).await()
                        if (results.isNotEmpty()) {
                            results[0].formattedAddress
                        } else {
                            "Unknown Location"
                        }
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error geocoding: ${e.message}")
                        "Error fetching location"
                    }
                }
            } else {
                "No Location"
            }

            // Get app limits count
            val appLimitsSnapshot = db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .collection("App_time_limits")
                .get()
                .await()
            
            val appLimitCount = appLimitsSnapshot.size()

            _uiState.value = HomeUiState.Success(
                parentName = parentName,
                children = children,
                selectedChildId = childId,
                screenTime = screenTime,
                locationName = locationName,
                appLimitCount = appLimitCount
            )
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading card data: ${e.message}")
            _uiState.value = HomeUiState.Error("Error loading child data: ${e.message}")
        }
    }

    fun onChildSelected(childId: String) {
        viewModelScope.launch {
            // Update SharedPreferences
            sharedPreferences.edit()
                .putString(Constants.KEY_CHILD_ID, childId)
                .apply()

            // Reload data for selected child
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                loadCardData(childId, currentState.parentName, currentState.children)
            }
        }
    }

    fun scanQRCode(scannedChildId: String) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = HomeUiState.Error(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = HomeUiState.Error(Constants.ERROR_AUTH_FAILED)
                return@launch
            }

            try {
                // Check if child document exists
                val childDoc = db.collection(Constants.COLLECTION_CHILD)
                    .document(scannedChildId)
                    .get()
                    .await()

                if (!childDoc.exists()) {
                    _uiState.value = HomeUiState.Error("Invalid Child Profile")
                    return@launch
                }

                // Update parent's child_ids array
                db.collection(Constants.COLLECTION_PARENT)
                    .document(userId)
                    .update("child_ids", FieldValue.arrayUnion(scannedChildId))
                    .await()

                // Update SharedPreferences
                val storedChildIdsJson = sharedPreferences.getString(Constants.KEY_CHILD_IDS, null)
                val childIds = if (!storedChildIdsJson.isNullOrEmpty()) {
                    Gson().fromJson(storedChildIdsJson, Array<String>::class.java).toMutableList()
                } else {
                    mutableListOf()
                }
                
                childIds.add(scannedChildId)
                val updatedChildIdsJson = Gson().toJson(childIds)
                sharedPreferences.edit()
                    .putString(Constants.KEY_CHILD_IDS, updatedChildIdsJson)
                    .apply()

                // Reload home data
                loadHomeData()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error scanning QR: ${e.message}")
                _uiState.value = HomeUiState.Error("Error adding child: ${e.message}")
            }
        }
    }

    private fun storeChildIdsInSharedPrefs(childId: String, childIds: List<String>) {
        val childIdsJson = Gson().toJson(childIds)
        sharedPreferences.edit()
            .putString(Constants.KEY_CHILD_ID, childId)
            .putString(Constants.KEY_CHILD_IDS, childIdsJson)
            .apply()
    }
}
