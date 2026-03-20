package com.example.boardingscreen.utils

object Constants {
    // Firestore Collections
    const val COLLECTION_PARENT = "Parent"
    const val COLLECTION_CHILD = "Child"
    const val COLLECTION_GEO_DETAILS = "geo_details"
    const val COLLECTION_APP_TIME_LIMITS = "App_time_limits"
    
    // SharedPreferences Keys
    const val PREFS_USER = "user_prefs"
    const val PREFS_SERVICE = "service_pref"
    const val KEY_CHILD_ID = "child_id"
    const val KEY_CHILD_IDS = "child_ids"
    const val KEY_CHILD_NAME = "child_name"
    const val KEY_CHILD_EMAIL = "child_email"
    const val KEY_CHILD_GENDER = "child_gender"
    const val KEY_SECURE_PIN = "secure_pin"
    const val KEY_PARENT_NAME = "name"
    const val KEY_SERVICE_ENABLED = "is_service_enabled"
    
    // Error Messages
    const val ERROR_NO_INTERNET = "No internet connection"
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_GPS_DISABLED = "GPS is disabled"
    const val ERROR_CHILD_NOT_FOUND = "Child not found"
    const val ERROR_LOCATION_NOT_FOUND = "Location not available"
    const val ERROR_AUTH_FAILED = "Authentication failed"
    const val ERROR_INVALID_CREDENTIALS = "Invalid email or password"
    const val ERROR_ACCOUNT_CREATION_FAILED = "Account creation failed"
    const val ERROR_LOGOUT_FAILED = "Logout failed"
    const val ERROR_UNKNOWN = "Something went wrong"
    
    // UI Messages
    const val MSG_NO_CHILD_CONNECTED = "No child device connected"
    const val MSG_LOADING = "Loading..."
    const val MSG_SUCCESS = "Success"
}
