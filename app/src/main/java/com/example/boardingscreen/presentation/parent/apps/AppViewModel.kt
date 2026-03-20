package com.example.boardingscreen.presentation.parent.apps

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boardingscreen.data.model.AppTimeDetails
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppViewModel(
    private val sharedPreferences: SharedPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<AppUiState>()
    val uiState: LiveData<AppUiState> = _uiState

    private val db = FirebaseFirestore.getInstance()
    private var currentChildId: String? = null

    fun loadAppLimits() {
        viewModelScope.launch {
            _uiState.value = AppUiState.Loading

            if (!NetworkUtils.isNetworkAvailable(context)) {
                _uiState.value = AppUiState.NoInternet
                return@launch
            }

            currentChildId = sharedPreferences.getString(Constants.KEY_CHILD_ID, null)

            if (currentChildId.isNullOrEmpty()) {
                _uiState.value = AppUiState.NoChild
                return@launch
            }

            try {
                // Fetch installed apps count and app time details
                val childDoc = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .get()
                    .await()

                if (!childDoc.exists()) {
                    _uiState.value = AppUiState.Error("Child document not found")
                    return@launch
                }

                val installedApps = childDoc.get("installed_apps") as? List<String> ?: emptyList()
                val installedAppsCount = installedApps.size

                // Fetch app time limits
                val timeLimitsSnapshot = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                    .get()
                    .await()

                val timeLimitMap = mutableMapOf<String, Int>()
                for (doc in timeLimitsSnapshot) {
                    val appName = doc.getString("app_name") ?: continue
                    val timeLimit = doc.getLong("time_limit")?.toInt() ?: 0
                    timeLimitMap[appName] = timeLimit
                }

                val appTimeList = installedApps.map { appName ->
                    val timeLimit = timeLimitMap[appName] ?: 0
                    AppTimeDetails(appName, timeLimit)
                }

                _uiState.value = AppUiState.Success(installedAppsCount, appTimeList)
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error loading app limits: ${e.message}")
                _uiState.value = AppUiState.Error("Error fetching data: ${e.message}")
            }
        }
    }

    fun addOrUpdateAppLimit(appName: String, timeLimit: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                onError(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            if (currentChildId.isNullOrEmpty()) {
                onError("Child ID not found")
                return@launch
            }

            try {
                // Check if app already has a time limit
                val existingDocs = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                    .whereEqualTo("app_name", appName)
                    .get()
                    .await()

                if (existingDocs.isEmpty) {
                    // Create new document
                    val timeLimitData = hashMapOf(
                        "app_name" to appName,
                        "time_limit" to timeLimit
                    )
                    db.collection(Constants.COLLECTION_CHILD)
                        .document(currentChildId!!)
                        .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                        .add(timeLimitData)
                        .await()
                } else {
                    // Update existing document
                    val docId = existingDocs.documents[0].id
                    db.collection(Constants.COLLECTION_CHILD)
                        .document(currentChildId!!)
                        .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                        .document(docId)
                        .update("time_limit", timeLimit)
                        .await()
                }

                onSuccess()
                loadAppLimits() // Refresh the list
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error saving time limit: ${e.message}")
                onError("Error saving time limit: ${e.message}")
            }
        }
    }

    fun removeAppLimit(appName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                onError(Constants.ERROR_NO_INTERNET)
                return@launch
            }

            if (currentChildId.isNullOrEmpty()) {
                onError("Child ID not found")
                return@launch
            }

            try {
                val existingDocs = db.collection(Constants.COLLECTION_CHILD)
                    .document(currentChildId!!)
                    .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                    .whereEqualTo("app_name", appName)
                    .get()
                    .await()

                if (!existingDocs.isEmpty) {
                    val docId = existingDocs.documents[0].id
                    db.collection(Constants.COLLECTION_CHILD)
                        .document(currentChildId!!)
                        .collection(Constants.COLLECTION_APP_TIME_LIMITS)
                        .document(docId)
                        .delete()
                        .await()

                    onSuccess()
                    loadAppLimits() // Refresh the list
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error removing time limit: ${e.message}")
                onError("Error removing time limit: ${e.message}")
            }
        }
    }
}
