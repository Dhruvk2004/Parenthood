package com.example.boardingscreen.presentation.child

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChildViewModelFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val servicePreferences: SharedPreferences
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChildViewModel::class.java)) {
            return ChildViewModel(sharedPreferences, servicePreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
