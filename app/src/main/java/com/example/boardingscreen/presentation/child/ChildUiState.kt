package com.example.boardingscreen.presentation.child

import com.example.boardingscreen.data.model.Geofence_details

sealed class ChildUiState {
    object Loading : ChildUiState()
    object NoInternet : ChildUiState()
    data class ProfileLoaded(
        val name: String,
        val email: String,
        val gender: String?,
        val isServiceRunning: Boolean
    ) : ChildUiState()
    data class GeofencesLoaded(val geofences: List<Geofence_details>) : ChildUiState()
    object ServicesStarted : ChildUiState()
    object ServicesStopped : ChildUiState()
    object LogoutSuccess : ChildUiState()
    object PinVerified : ChildUiState()
    data class PinError(val message: String) : ChildUiState()
    data class Error(val message: String) : ChildUiState()
}
