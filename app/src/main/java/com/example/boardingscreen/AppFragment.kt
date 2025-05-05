package com.example.boardingscreen

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppFragment : Fragment() {

    private lateinit var total_app:AppCompatTextView
    private lateinit var nochild_txt:TextView
    private lateinit var appList: ListView
    private lateinit var appTimeAdapter: AppTimeAdapter
   var parentid:String?=null
    private var auth = FirebaseAuth.getInstance().currentUser
    private var db = FirebaseFirestore.getInstance()
    var child_id:String? = null
    private var appTimeList = ArrayList<AppTimeDetails>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_app, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        total_app=view.findViewById(R.id.total_apps_number)
        nochild_txt=view.findViewById(R.id.no_child_txt)
        appList=view.findViewById(R.id.app_list)

        parentid=auth!!.uid
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        child_id = sharedPref.getString("child_id", null)

        if (child_id == null || child_id == "") {
            total_app.text = "No child selected"
            nochild_txt.visibility=View.VISIBLE
        } else {
            appList.visibility=View.VISIBLE
            fetchInstalledAppsCount(child_id!!)
            fetchAppTimeDetails(child_id!!)
        }
    }

    fun fetchInstalledAppsCount(child_id: String) {
        db.collection("Child").document(child_id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the installed_apps array
                    val installedApps = document.get("installed_apps") as? List<*>

                    // Check if installed_apps is not null, and count the size
                    val count = installedApps?.size ?: 0

                    // Set the count as the text of total_app TextView
                    total_app.text = "$count Installed Apps"
                } else {
                    total_app.text = "No installed apps found"
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors (e.g., Firestore retrieval failed)
                total_app.text = "Error fetching data"
                Log.e("Firestore", "Error fetching installed_apps count", exception)
            }
    }


    private fun fetchAppTimeDetails(child_id: String) {
        // Fetch the installed_apps array first
        db.collection("Child").document(child_id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the installed apps array
                    val installedApps =
                        document.get("installed_apps") as? List<String> ?: emptyList()

                    // Now check the App_time_limits collection for time limits
                    db.collection("Child").document(child_id)
                        .collection("App_time_limits")
                        .get()
                        .addOnSuccessListener { result ->
                            // Clear the list before adding new data
                            appTimeList.clear()

                            // Create a map of app names and their time limits
                            val timeLimitMap = mutableMapOf<String, Int>()
                            for (doc in result) {
                                val appName = doc.getString("app_name") ?: continue
                                val timeLimit = doc.getLong("time_limit")?.toInt() ?: 0
                                timeLimitMap[appName] = timeLimit
                            }

                            // Loop through the installed apps
                            for (appName in installedApps) {
                                // Check if the app has a time limit set
                                val timeLimit =
                                    timeLimitMap[appName] ?: 0 // Default to 0 if no time limit
                                appTimeList.add(AppTimeDetails(appName, timeLimit))
                            }

                            // Check if the fragment is still added before setting up the adapter
                            if (isAdded) {
                                context?.let {
                                    appTimeAdapter = AppTimeAdapter(
                                        it,
                                        appTimeList,
                                        { appTimeDetails ->
                                            showTimeSetDialog(appTimeDetails)
                                        },
                                        { appTimeDetails ->
                                            // Handle the delete logic here
                                            removeTimeLimit(appTimeDetails)
                                        }
                                    )
                                    appList.adapter = appTimeAdapter
                                    appTimeAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error fetching app_time_limits", exception)
                        }
                } else {
                    total_app.text = "No installed apps found"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching installed_apps", exception)
            }
    }

    private fun removeTimeLimit(appTimeDetails: AppTimeDetails) {
        db.collection("Child").document(child_id!!)
            .collection("App_time_limits")
            .whereEqualTo("app_name", appTimeDetails.appName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // Assuming the document ID is unique for each app_name
                    val docId = result.documents[0].id
                    db.collection("Child").document(child_id!!)
                        .collection("App_time_limits")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Time limit removed", Toast.LENGTH_SHORT).show()
                            fetchAppTimeDetails(child_id!!) // Refresh the list after deletion
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error removing time limit", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching app_time_limits", exception)
            }
    }



    fun showTimeSetDialog(appTimeDetails: AppTimeDetails){
        val dialogView = layoutInflater.inflate(R.layout.add_app_time_dialog, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        // Get references to views
        val timeSeekBar = dialog.findViewById<SeekBar>(R.id.time_seekbar)
        val timeTextView = dialog.findViewById<TextView>(R.id.rad_txt)
        val saveButton = dialog.findViewById<Button>(R.id.save_btn)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_btn)

        // Check if the app already has a time limit set
        val currentTimeLimit = appTimeDetails.timeLimitSet
        if (currentTimeLimit > 0) {
            timeSeekBar.progress = currentTimeLimit
            timeTextView.text = "Time Limit: ${currentTimeLimit} minutes"
        } else {
            timeSeekBar.progress = 10
            timeTextView.text = "Time Limit: 10 minutes"
        }

        // Update the TextView as the SeekBar value changes
        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                timeTextView.text = "Time Limit: ${progress} minutes"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
        })

        // Handle Save button click
        saveButton.setOnClickListener {
            val newTimeLimit = timeSeekBar.progress

            // Check if the app already has a time limit in Firestore
            db.collection("Child").document(child_id!!)
                .collection("App_time_limits")
                .whereEqualTo("app_name", appTimeDetails.appName)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        // No existing document, create a new one
                        val timeLimitData = hashMapOf(
                            "app_name" to appTimeDetails.appName,
                            "time_limit" to newTimeLimit
                        )
                        db.collection("Child").document(child_id!!)
                            .collection("App_time_limits")
                            .add(timeLimitData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Time limit saved!", Toast.LENGTH_SHORT).show()
                                fetchAppTimeDetails(child_id!!)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error adding time limit", exception)
                            }
                    } else {
                        // Document exists, update the time limit
                        val docId = result.documents[0].id
                        db.collection("Child").document(child_id!!)
                            .collection("App_time_limits")
                            .document(docId)
                            .update("time_limit", newTimeLimit)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Time limit updated!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error updating time limit", exception)
                            }
                    }
                    dialog.dismiss() // Close the dialog after saving
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error fetching app_time_limits", exception)
                }
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }




}
