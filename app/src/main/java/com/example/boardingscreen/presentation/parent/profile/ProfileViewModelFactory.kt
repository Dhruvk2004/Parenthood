package com.example.boardingscreen.presentation.parent.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProfileViewModelFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(sharedPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
