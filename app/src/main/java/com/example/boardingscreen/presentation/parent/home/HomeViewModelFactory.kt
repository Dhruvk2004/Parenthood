package com.example.boardingscreen.presentation.parent.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardingscreen.data.repository.ChildRepository
import com.example.boardingscreen.data.repository.ChildRepositoryImpl

class HomeViewModelFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val childRepository: ChildRepository = ChildRepositoryImpl()
            return HomeViewModel(childRepository, sharedPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
