package com.example.boardingscreen.presentation.parent.profile

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object NoInternet : ProfileUiState()
    data class Success(
        val name: String,
        val email: String,
        val gender: String?
    ) : ProfileUiState()
    object LogoutSuccess : ProfileUiState()
    object ChildAdded : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
