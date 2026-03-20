package com.example.boardingscreen.presentation.parent.home

import com.example.boardingscreen.data.model.Child

sealed class HomeUiState {
    data class Loading(val parentName: String? = null) : HomeUiState()
    data class NoInternet(val parentName: String? = null) : HomeUiState()
    data class NoChild(val parentName: String) : HomeUiState()
    data class Success(
        val parentName: String,
        val children: List<Child>,
        val selectedChildId: String,
        val screenTime: String?,
        val locationName: String?,
        val appLimitCount: Int
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
