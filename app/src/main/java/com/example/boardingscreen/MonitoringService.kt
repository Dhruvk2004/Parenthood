package com.example.boardingscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class MonitoringService : Service() {

    private lateinit var db: FirebaseFirestore
    private lateinit var childId: String

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("Monitoring Service", "Service for instant notifications"))
        db = FirebaseFirestore.getInstance()
        childId = getChildIdFromPreferences() // Implement this method
        if (childId.isEmpty()) {
            Log.e("MonitoringService", "Child ID is empty. Stopping service.")
            stopSelf() // Stop service if childId is not found
            return
        }
        listenToAppEvents()
        listenToGeofenceEvents()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Monitoring Service"
            val descriptionText = "Channel for monitoring service notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("your_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, message: String): Notification {
        val intent = Intent(this, ParentActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "your_channel_id")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showNotification(title: String, message: String) {
        val notification = createNotification(title, message)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getChildIdFromPreferences(): String {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("child_id", "") ?: ""
    }

    // Method to listen to app_events
    private fun listenToAppEvents() {
        val childDocRef = db.collection("Child").document(childId)

        // Listen for document changes instead of the whole document snapshot
        childDocRef.collection("AppEvents") // Assuming "AppEvents" is a subcollection for storing events
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("MonitoringService", "Listen failed for AppEvents.", error)
                    return@addSnapshotListener
                }

                // Handle document changes (new events only)
                for (docChange in snapshot.documentChanges) {
                    if (docChange.type == DocumentChange.Type.ADDED) {
                        // Get data from the new event
                        val action = docChange.document.getString("action")
                        val appName = docChange.document.getString("app_name")
                        val timestamp = docChange.document.getString("timestamp")

                        Log.d("MonitoringService", "New AppEvent detected: $action $appName at $timestamp")

                        // Ensure that the timestamp exists and is a new event
                        showAppActivityNotification(action!!, appName!!, timestamp!!)
                    }
                }
            }
    }

    // Method to listen to geofence_events
    private fun listenToGeofenceEvents() {
        db.collection("Child").document(childId).collection("geo_details")
            .get()
            .addOnSuccessListener { geoDetailsSnapshot ->

                val geoIds = geoDetailsSnapshot.documents.map { it.id }

                db.collection("geo_events")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) {
                            Log.w("MonitoringService", "Listen failed.", e)
                            return@addSnapshotListener
                        }

                        for (docChange in snapshot.documentChanges) {
                            if (docChange.type == DocumentChange.Type.ADDED) {
                                val geoFenceId = docChange.document.getString("geofence_id")
                                val timestamp = docChange.document.getString("timestamp")
                                val type = docChange.document.getString("type")
                                Log.d("MonitoringService", "Geofence event added: $geoFenceId at $timestamp")

                                if (geoIds.contains(geoFenceId)) {
                                    Log.d("MonitoringService", "trigerring geofence notification")
                                    showGeofenceNotification(type!!, geoFenceId!!, timestamp!!)

                                }
                            }
                        }
                    }
            }.addOnFailureListener { e ->
                Log.e("MonitoringService", "Failed to retrieve geo_details.", e)
            }
    }


    private fun showAppActivityNotification(action: String, appName: String, timestamp: String) {
        val message = "$action $appName at $timestamp"
        // Build and show the notification
        showNotification("App Activity Detected", message)
    }

    private fun showGeofenceNotification(type: String, geoFenceId: String, timestamp: String) {
        val message = "$type at geofence ID $geoFenceId at $timestamp"
        // Build and show the notification
        showNotification("Geofence Event Detected", message)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listening to events if necessary
    }
}
