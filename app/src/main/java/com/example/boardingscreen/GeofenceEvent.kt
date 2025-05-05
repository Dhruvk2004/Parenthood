package com.example.boardingscreen

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class GeofenceEvent : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val userid: String? = FirebaseAuth.getInstance().currentUser?.uid
    private val geofences = mutableListOf<Geo_detail>()
    private val geofenceFlags = mutableMapOf<String, Boolean>()
    private var currentLocation: GeoPoint? = null
    private var geofenceListener: ListenerRegistration? = null
    private var timer: Timer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedPreferences = getSharedPreferences("GeofencePrefs", MODE_PRIVATE)
        geofenceFlags.clear() // Clear the existing map
        geofences.forEach { geofence ->
            val flag = sharedPreferences.getBoolean(geofence.geofenceId, false)
            geofenceFlags[geofence.geofenceId] = flag
        }
        fetchGeofences()
        timer= Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d("Geo_event","running geofence check")
                fetchCurrentLocation()
                listenForGeofenceUpdates()
                checkGeofences()
            }
        }, 0, 3000) // Run every 1 minute
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        geofenceListener?.remove() // Remove the listener when the service is destroyed
        timer?.cancel()

        val sharedPreferences = getSharedPreferences("GeofencePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        geofenceFlags.forEach { (key, value) ->
            editor.putBoolean(key, value)
        }
        editor.apply()

        Log.d("Geofence_event","Service destroyed")

    }

    private fun listenForGeofenceUpdates() {
        // Access the geo_details subcollection under the specific child's document
        geofenceListener = db.collection("Child")
            .document(userid!!)
            .collection("geo_details")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    geofences.clear()
                    val newGeofenceFlags = mutableMapOf<String, Boolean>()
                    for (document in snapshots) {
                        val name = document.getString("Name") ?: continue
                        val place = document.getString("place") ?: continue
                        val radius = document.getDouble("radius")?.toFloat() ?: continue
                        val geopoint = document.getGeoPoint("geopoint") ?: continue

                        val geo_id = document.id

                        // Create a geofence object with the data
                        val geofence = Geo_detail(
                            geofenceId = geo_id,
                            latitude = geopoint.latitude,
                            longitude = geopoint.longitude,
                            radius = radius
                        )

                        // Add the geofence to the list and update the flag
                        geofences.add(geofence)
                        // Maintain existing flag if it already exists
                        newGeofenceFlags[geo_id] = geofenceFlags[geo_id] ?: false
                    }
                    geofenceFlags.clear()
                    geofenceFlags.putAll(newGeofenceFlags)
                    Log.d("Geo_event","Geofences updated: $geofences")
                }
            }
    }

    private fun fetchGeofences() {
        // Access the geo_details subcollection under the specific child's document
        db.collection("Child")
            .document(userid!!)
            .collection("geo_details")
            .get()
            .addOnSuccessListener { documents ->
                geofences.clear()
                geofenceFlags.clear()

                for (document in documents) {
                    val place = document.getString("place") ?: continue
                    val radius = document.getDouble("radius")?.toFloat() ?: continue
                    val geopoint = document.getGeoPoint("geopoint") ?: continue

                    val geo_id=document.id

                    val geofence = Geo_detail(
                        geofenceId = geo_id,
                        latitude = geopoint.latitude,
                        longitude = geopoint.longitude,
                        radius = radius
                    )

                    // Add the geofence to the list and update the flag
                    geofences.add(geofence)
                    geofenceFlags[geo_id] = false
                    Log.d("Geo_event","successfully fetched geofence")
                }
            }
            .addOnFailureListener { exception ->
                println("Error fetching geofences: $exception")
            }
    }

    private fun fetchCurrentLocation() {
        db.collection("Child")
            .document(userid!!) // Use the current child document (current user's ID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentLocation = document.getGeoPoint("location")
                    if (currentLocation != null) {
                        Log.d("Geo_event","Current location fetched: $currentLocation")
                    } else {
                        Log.d("Geo_event","Location field is null or doesn't exist")
                    }
                } else {
                    Log.d("Geo_event","No such document found for the child")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Geo_event","Error fetching current location: $exception")
            }
    }

    private fun checkGeofences() {
        // Get current location (mocked for this example)
        val location = currentLocation ?: return

        Log.d("Geofence_Event", "Current location: ${location.latitude}, ${location.longitude}")
        Log.d("Geofence_Event", "Geofence flags: $geofenceFlags")
        geofences.forEach { geofence ->
            val distance = calculateDistance(
                location.latitude,location.longitude,
                geofence.latitude, geofence.longitude
            )

            Log.d("Geofence_Event", "Distance to geofence ${geofence.geofenceId}: $distance, Radius: ${geofence.radius}")

            val isInside = distance <= geofence.radius

            if (isInside && geofenceFlags[geofence.geofenceId] == false) {

                Log.d("Geofence_Event", "Entering geofence ${geofence.geofenceId}")
                // Enter event
                geofenceFlags[geofence.geofenceId] = true
                logGeofenceEvent(geofence.geofenceId, "Enter")
            } else if (!isInside && geofenceFlags[geofence.geofenceId] == true) {
                // Exit event

                Log.d("Geofence_Event", "Exiting geofence ${geofence.geofenceId}")
                geofenceFlags[geofence.geofenceId] = false
                logGeofenceEvent(geofence.geofenceId, "Exit")
            }
        }
        Log.d("Geofence_Event", "Updated geofence flags: $geofenceFlags")
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun logGeofenceEvent(geoDetailId: String, eventType: String) {
        Log.d("GeofenceEvent", "Triggering the geofence event: $eventType for geofence $geoDetailId")
        val sdf = SimpleDateFormat("HH.mm", Locale.getDefault()) // Create a date formatter
        val currentTime = sdf.format(Date()) // Get the current time in hh.mm format

        val event = hashMapOf(
            "geofence_id" to geoDetailId,    // The ID of the geofence
            "type" to eventType,             // Type of event (e.g., ENTER, EXIT)
            "timestamp" to currentTime // Time of event
        )

        // Adding the event to the 'geo_events' collection
        db.collection("geo_events")
            .add(event)
            .addOnSuccessListener { documentReference ->
                Log.d("GeofenceService", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceService", "Error adding document: $e")
            }
    }
}