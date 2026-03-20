package com.example.boardingscreen.presentation.parent.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppViewModelFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(sharedPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
