package com.example.boardingscreen.presentation.parent.map

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardingscreen.data.model.Geofence
import com.example.boardingscreen.data.repository.ChildRepository
import com.example.boardingscreen.data.repository.GeofenceRepository
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.NetworkUtils
import com.example.boardingscreen.utils.Result
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel(
    private val childRepository: ChildRepository,
    private val geofenceRepository: GeofenceRepository,
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<MapUiState>()
    val uiState: LiveData<MapUiState> = _uiState

    private val db = FirebaseFirestore.getInstance()
    private var currentChildId: String? = null

    fun loadChildLocation() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading

            // Check network connectivity
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = MapUiState.NoInternet
                return@launch
            }

            // Get child ID from SharedPreferences
            currentChildId = sharedPreferences.getString(Constants.KEY_CHILD_ID, null)
            
            if (currentChildId.isNullOrEmpty()) {
                _uiState.value = MapUiState.NoChild
                return@launch
            }

            // Load child location
            when (val result = childRepository.getChildLocation(currentChildId!!)) {
                is Result.Success -> {
                    val (lat, lng) = result.data
                    val location = LatLng(lat, lng)
                    
                    // Load geofences
                    loadGeofences(location)
                }
                is Result.Error -> {
                    _uiState.value = MapUiState.Error(result.exception.message ?: Constants.ERROR_UNKNOWN)
                }
                is Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    private suspend fun loadGeofences(childLocation: LatLng) {
        if (currentChildId == null) {
            _uiState.value = MapUiState.Error("Child ID not found")
            return
        }

        try {
            // Fetch geofences with geopoint data from Firestore
            val querySnapshot = db.collection(Constants.COLLECTION_CHILD)
                .document(currentChildId!!)
                .collection(Constants.COLLECTION_GEO_DETAILS)
                .get()
                .await()

            val geofenceDataList = mutableListOf<GeofenceData>()

            for (document in querySnapshot.documents) {
                val geoId = document.id
                val geoName = document.getString("Name")
                val placeName = document.getString("place")
                val geoPoint = document.getGeoPoint("geopoint")
                val radius = document.getDouble("radius")?.toInt()

                if (geoId != null && geoName != null && placeName != null && geoPoint != null && radius != null) {
                    val geofenceData = GeofenceData(
                        geofenceId = geoId,
                        name = geoName,
                        placeName = placeName,
                        location = LatLng(geoPoint.latitude, geoPoint.longitude),
                        radius = radius
                    )
                    geofenceDataList.add(geofenceData)
                }
            }

            _uiState.value = MapUiState.ChildLocationLoaded(childLocation, geofenceDataList)
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error loading geofences: ${e.message}")
            _uiState.value = MapUiState.Error("Error loading geofences: ${e.message}")
        }
    }

    fun addGeofence(name: String, placeName: String, location: LatLng, radius: Int) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = MapUiState.Error(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            if (currentChildId.isNullOrEmpty()) {
                _uiState.value = MapUiState.Error("Child ID not found")
                return@launch
            }

            try {
                // Store geofence with geopoint in Firestore
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val geoDetails = hashMapOf(
                    "Name" to name,
                    "place" to placeName,
                    "geopoint" to geoPoint,
                    "radius" to radius
                )

                val docRef = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .collection(Constants.COLLECTION_GEO_DETAILS)
                    .add(geoDetails)
                    .await()

                val geofenceData = GeofenceData(
                    geofenceId = docRef.id,
                    name = name,
                    placeName = placeName,
                    location = location,
                    radius = radius
                )

                _uiState.value = MapUiState.GeofenceAdded(geofenceData)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error adding geofence: ${e.message}")
                _uiState.value = MapUiState.Error("Failed to save geofence: ${e.message}")
            }
        }
    }

    fun deleteGeofence(geofenceId: String) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = MapUiState.Error(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            if (currentChildId.isNullOrEmpty()) {
                _uiState.value = MapUiState.Error("Child ID not found")
                return@launch
            }

            when (val result = geofenceRepository.deleteGeofence(currentChildId!!, geofenceId)) {
                is Result.Success -> {
                    _uiState.value = MapUiState.GeofenceDeleted(geofenceId)
                    
                    // Reload child location to refresh the map
                    loadChildLocation()
                }
                is Result.Error -> {
                    _uiState.value = MapUiState.Error(result.exception.message ?: "Failed to remove geofence")
                }
                is Result.Loading -> {
                    // Handle loading if needed
                }
            }
        }
    }

    fun getGeofencesList(onSuccess: (List<GeofenceData>) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                onError(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            if (currentChildId.isNullOrEmpty()) {
                onError("Child ID not found")
                return@launch
            }

            try {
                val querySnapshot = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .collection(Constants.COLLECTION_GEO_DETAILS)
                    .get()
                    .await()

                val geofenceDataList = mutableListOf<GeofenceData>()

                for (document in querySnapshot.documents) {
                    val geoId = document.id
                    val geoName = document.getString("Name")
                    val placeName = document.getString("place")
                    val geoPoint = document.getGeoPoint("geopoint")
                    val radius = document.getDouble("radius")?.toInt()

                    if (geoId != null && geoName != null && placeName != null && geoPoint != null && radius != null) {
                        val geofenceData = GeofenceData(
                            geofenceId = geoId,
                            name = geoName,
                            placeName = placeName,
                            location = LatLng(geoPoint.latitude, geoPoint.longitude),
                            radius = radius
                        )
                        geofenceDataList.add(geofenceData)
                    }
                }

                onSuccess(geofenceDataList)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error loading geofences list: ${e.message}")
                onError("Failed to retrieve geofence details: ${e.message}")
            }
        }
    }
}
