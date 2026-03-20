package com.example.boardingscreen.presentation.parent.map

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.boardingscreen.data.repository.ChildRepository
import com.example.boardingscreen.data.repository.ChildRepositoryImpl
import com.example.boardingscreen.data.repository.GeofenceRepository
import com.example.boardingscreen.data.repository.GeofenceRepositoryImpl

class MapViewModelFactory(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            val childRepository: ChildRepository = ChildRepositoryImpl()
            val geofenceRepository: GeofenceRepository = GeofenceRepositoryImpl()
            return MapViewModel(childRepository, geofenceRepository, sharedPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
