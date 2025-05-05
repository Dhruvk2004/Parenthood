package com.example.boardingscreen

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import org.greenrobot.eventbus.EventBus

class UpdateLocationService : Service() {

    companion object{
        const val CHANNEL_ID="12345"
        const val NOTIFICATION_ID=12345
    }
    private var fusedLocationProviderClient: FusedLocationProviderClient?=null
    private var locationCallback: LocationCallback?=null
    private var locationRequest: LocationRequest?=null
    private var notificationManager: NotificationManager?=null
    private var location: Location?=null

    private var db= Firebase.firestore

    private val child_id: String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun updateFirestoreLocation(latitude: Double?, longitude: Double?) {
        if (child_id.isNullOrEmpty()) {
            Log.e("LocationService", "User not authenticated")
            return
        }

        if (latitude == null || longitude == null) {
            Log.e("LocationService", "Invalid location data")
            return
        }

        val geoPoint = GeoPoint(latitude, longitude)

        db.collection("Child").document(child_id)
            .update("location", geoPoint)
            .addOnSuccessListener {
                //Toast.makeText(this, "Location Updated", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                // Handle failure if needed
                Toast.makeText(this, "Failed to update location", Toast.LENGTH_LONG).show()
            }
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,3000)
                .build()
        locationCallback = object: LocationCallback(){
            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
            }

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult)
            }
        }

        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(CHANNEL_ID,"location",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(notificationChannel)
        }

    }

    @SuppressLint("MissingPermission")
    fun createLocationRequest(){
        try{
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest!!,locationCallback!!,null
            )
        }
        catch(e:Exception){
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun removeLocationUpdates(){
        locationCallback?.let {
            fusedLocationProviderClient?.removeLocationUpdates(it)
        }
        stopForeground(true)
        stopSelf()
    }

    private fun onNewLocation(locationResult: LocationResult) {
        location=locationResult.lastLocation

        updateFirestoreLocation(location?.latitude, location?.longitude)
        EventBus.getDefault().post(UpdateLocationEvent(
            latitude = location?.latitude,
            longitude = location?.longitude
        ))

        startForeground(NOTIFICATION_ID,getNotification())
    }

    fun getNotification(): Notification {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Location")
            .setContentText(
                "Parenthood is securely reading the device current location"
            )
            .setSmallIcon(R.mipmap.logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            notification.setChannelId(CHANNEL_ID)
        }
        return notification.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createLocationRequest()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {

        super.onDestroy()
        removeLocationUpdates()
    }
}