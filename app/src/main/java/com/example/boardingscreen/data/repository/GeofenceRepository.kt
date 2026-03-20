package com.example.boardingscreen.data.repository

import com.example.boardingscreen.data.model.Geofence
import com.example.boardingscreen.utils.Result

/**
 * Repository interface for geofence-related data operations.
 * Provides methods to manage geofences for child location monitoring.
 */
interface GeofenceRepository {
    /**
     * Fetches all geofences for a specific child.
     * @param childId The unique identifier of the child
     * @return Result containing a list of Geofence objects or an error
     */
    suspend fun getGeofences(childId: String): Result<List<Geofence>>
    
    /**
     * Adds a new geofence for a child.
     * @param childId The unique identifier of the child
     * @param geofence The geofence to add
     * @return Result indicating success or an error
     */
    suspend fun addGeofence(childId: String, geofence: Geofence): Result<Unit>
    
    /**
     * Deletes a geofence.
     * @param childId The unique identifier of the child
     * @param geofenceId The unique identifier of the geofence to delete
     * @return Result indicating success or an error
     */
    suspend fun deleteGeofence(childId: String, geofenceId: String): Result<Unit>
}
