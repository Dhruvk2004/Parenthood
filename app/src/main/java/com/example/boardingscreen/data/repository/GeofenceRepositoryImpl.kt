package com.example.boardingscreen.data.repository

import com.example.boardingscreen.data.model.Geofence
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

class GeofenceRepositoryImpl : GeofenceRepository {
    
    private val db = FirebaseFirestore.getInstance()
    
    override suspend fun getGeofences(childId: String): Result<List<Geofence>> {
        return try {
            val querySnapshot = db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .collection(Constants.COLLECTION_GEO_DETAILS)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val geofenceList = mutableListOf<Geofence>()
                
                for (document in querySnapshot.documents) {
                    val geoId = document.id
                    val geoName = document.getString("Name")
                    val placeName = document.getString("place")
                    val radius = document.getDouble("radius")?.toInt()
                    
                    if (geoId != null && geoName != null && placeName != null && radius != null) {
                        val geofence = Geofence(
                            geofenceId = geoId,
                            geofenceName = geoName,
                            locationName = placeName,
                            radius = radius
                        )
                        geofenceList.add(geofence)
                    }
                }
                
                Result.Success(geofenceList)
            } else {
                // No geofences found - return empty list
                Result.Success(emptyList())
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
    
    override suspend fun addGeofence(childId: String, geofence: Geofence): Result<Unit> {
        return try {
            // Note: The Geofence model doesn't include geopoint, but the Firestore structure requires it.
            // This will need to be addressed when MapFragment is refactored in Phase 4.
            // For now, we'll store what we can from the Geofence model.
            val geoDetails = hashMapOf(
                "Name" to geofence.geofenceName,
                "place" to geofence.locationName,
                "radius" to geofence.radius
            )
            
            db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .collection(Constants.COLLECTION_GEO_DETAILS)
                .add(geoDetails)
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
    
    override suspend fun deleteGeofence(childId: String, geofenceId: String): Result<Unit> {
        return try {
            db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .collection(Constants.COLLECTION_GEO_DETAILS)
                .document(geofenceId)
                .delete()
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
}
