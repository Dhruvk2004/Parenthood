package com.example.boardingscreen.presentation.parent.map

import com.example.boardingscreen.data.model.Geofence
import com.google.android.gms.maps.model.LatLng

sealed class MapUiState {
    object Loading : MapUiState()
    object NoInternet : MapUiState()
    object NoChild : MapUiState()
    data class ChildLocationLoaded(
        val location: LatLng,
        val geofences: List<GeofenceData>
    ) : MapUiState()
    data class GeofenceAdded(val geofence: GeofenceData) : MapUiState()
    data class GeofenceDeleted(val geofenceId: String) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

data class GeofenceData(
    val geofenceId: String,
    val name: String,
    val placeName: String,
    val location: LatLng,
    val radius: Int
)
