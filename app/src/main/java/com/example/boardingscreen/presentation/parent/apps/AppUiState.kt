package com.example.boardingscreen.presentation.parent.apps

import com.example.boardingscreen.data.model.AppTimeDetails

sealed class AppUiState {
    object Loading : AppUiState()
    object NoInternet : AppUiState()
    object NoChild : AppUiState()
    data class Success(
        val installedAppsCount: Int,
        val appTimeList: List<AppTimeDetails>
    ) : AppUiState()
    data class Error(val message: String) : AppUiState()
}
