package com.example.boardingscreen

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.*
import androidx.fragment.app.*
import com.google.android.gms.common.moduleinstall.*
import com.google.android.gms.tasks.Tasks
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.GeocodingResult
import com.google.maps.model.LatLng
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.*

class HomeFragment : Fragment() {

    private var isScannerInstalled=false
    lateinit var name_txt:TextView
    lateinit var noChildProfileText: TextView
    lateinit var addChildButton: AppCompatButton
    lateinit var scanner: GmsBarcodeScanner
    lateinit var child_list:Spinner
    lateinit var child_card_head:AppCompatTextView
    lateinit var child_info_head:AppCompatTextView
    lateinit var time_card:MaterialCardView
    lateinit var location_card:MaterialCardView
    lateinit var app_time_limit_card:MaterialCardView
    lateinit var card_bg:FragmentContainerView
    lateinit var child_card:MaterialCardView
    lateinit var noChildImage:AppCompatImageView
    private var auth = FirebaseAuth.getInstance().currentUser
    lateinit var sharedPref:SharedPreferences
    lateinit var time_value_txt:TextView
    lateinit var place_txt:TextView
    lateinit var limit_value_txt:TextView
    private var db = FirebaseFirestore.getInstance()
    var child_id:String=""
    lateinit var storedChildIds:MutableList<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noChildProfileText = view.findViewById(R.id.no_child_profile_text)
        addChildButton = view.findViewById(R.id.add_child_btn)
        noChildImage = view.findViewById(R.id.no_child_image)
        name_txt=view.findViewById(R.id.name_label)
        child_list=view.findViewById(R.id.child_id_spinner)
        child_card=view.findViewById(R.id.addchild_card)
        card_bg=view.findViewById(R.id.card_bg_id)
        child_card_head=view.findViewById(R.id.child_head)
        child_info_head=view.findViewById(R.id.child_info_head)
        time_card=view.findViewById(R.id.app_time_card)
        location_card=view.findViewById(R.id.location_card)
        app_time_limit_card=view.findViewById(R.id.total_app_limit_card)
        time_value_txt=view.findViewById(R.id.time_value_txt)
        place_txt=view.findViewById(R.id.place_txt)
        limit_value_txt=view.findViewById(R.id.total_limit_value_txt)
        getParentName()

        installGoogleScanner()
        val options=intializaGoogleScanner()
        scanner= GmsBarcodeScanning.getClient(requireContext(),options)

        sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentChildId = sharedPref.getString("child_id", null) // Default value is null if not found

        if (currentChildId != null) {
            // Perform action if child_id is found
            Log.d("ChildProfile", "Current selected child ID: $currentChildId")
            // You can use this child ID to display data or set it in the child_list spinner, etc.
        } else {
            Log.d("ChildProfile", "No child ID found in SharedPreferences")
        }

        addChildButton.setOnClickListener {
            addChildDevice()
        }
        checkChildProfile()
    }

    private fun getParentName() {
        // Get the SharedPreferences instance
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedName = sharedPreferences.getString("name", null)

        if (savedName != null) {
            // If the name is already stored in SharedPreferences, set it to name_txt
            name_txt.text = savedName
        } else {
            val userId = auth!!.uid

            if (userId != null) {
                // Fetch the document from the Parent collection
                db.collection("Parent")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val parentName = documentSnapshot.getString("name") // Assuming "name" field exists
                            if (parentName != null) {
                                // Update the name_txt TextView
                                name_txt.text = parentName

                                // Store the name in SharedPreferences for future use
                                val editor = sharedPreferences.edit()
                                editor.putString("name", parentName)
                                editor.apply()
                            } else {
                                Log.d("HomeFragment", "Name field is missing in the Parent document.")
                            }
                        } else {
                            Log.d("HomeFragment", "Parent document does not exist.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Error fetching Parent document: ${e.message}")
                    }
            } else {
                Log.e("HomeFragment", "User ID is null.")
            }
        }
    }


    private fun intializaGoogleScanner(): GmsBarcodeScannerOptions {
        return GmsBarcodeScannerOptions.Builder().
        setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom().build()
    }

    private fun installGoogleScanner() {
        val moduleInstall= ModuleInstall.getClient(requireContext())
        val moduleInstallRequest= ModuleInstallRequest.newBuilder()
            .addApi(GmsBarcodeScanning.getClient(requireContext()))
            .build()

        moduleInstall.installModules(moduleInstallRequest).addOnSuccessListener {
            isScannerInstalled=true
        }.addOnFailureListener {
            isScannerInstalled=false
            Toast.makeText(requireContext(),it.message,Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun checkChildProfile() {
        val userId = auth?.uid
        if (userId != null) {
            // Check if child_id is stored in Shared Preferences
            val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val storedChildId = sharedPreferences.getString("child_id", null)
            val storedChildIdsJson = sharedPreferences.getString("child_ids", null)

            if (!storedChildIdsJson.isNullOrEmpty()) {

                child_info_head.visibility = View.VISIBLE
                time_card.visibility=View.VISIBLE
                location_card.visibility=View.VISIBLE
                app_time_limit_card.visibility=View.VISIBLE
                card_bg.visibility=View.VISIBLE
                child_card_head.visibility=View.VISIBLE
                child_card.visibility = View.VISIBLE
                storedChildIds = Gson().fromJson(storedChildIdsJson, Array<String>::class.java).toMutableList()

                if (!storedChildId.isNullOrEmpty()) {
                    child_id = storedChildId
                } else {
                    child_id = storedChildIds[0] // Set to first child if no specific child_id is stored
                }
                updateUIWithChildDetails(child_id, storedChildIds)
                updateAllCards(child_id)
            } else {
                // Child ID not in Shared Preferences, fetch from Firestore
                db.collection("Parent").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val childIds = document.get("child_ids") as? List<String>
                               if (childIds.isNullOrEmpty()) {
                                // No child profile connected
                                noChildProfileText.visibility = View.VISIBLE
                                addChildButton.visibility = View.VISIBLE
                                noChildImage.visibility = View.VISIBLE
                            } else {
                                child_card_head.visibility=View.VISIBLE
                                card_bg.visibility=View.VISIBLE
                                child_card.visibility = View.VISIBLE
                                child_info_head.visibility = View.VISIBLE
                                time_card.visibility=View.VISIBLE
                                location_card.visibility=View.VISIBLE
                                app_time_limit_card.visibility=View.VISIBLE
                                child_id = childIds[0]
                                storeChildIdInSharedPrefs(child_id,childIds)
                                updateUIWithChildDetails(child_id,childIds)
                                updateAllCards(child_id)
                            }
                        }
                    }
            }
        }
    }

    private fun updateUIWithChildDetails(childId: String, childIds:List<String>){
        // Prepare a list to store child names
        val childList = mutableListOf<Child>()
        // Fetch names for all child IDs
        val childCollection = db.collection("Child")
        val tasks = childIds.map { id ->
            childCollection.document(id).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val childName = document.getString("name")
                    if (childName != null) {
                        childList.add(Child(id,childName))
                    }
                }
            }
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            val childNames = childList.map { it.name }
            // Create an ArrayAdapter using the child names with default spinner layout
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, childNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            child_list.adapter = adapter

            // Set the default selected child if there's a stored child ID
            if (!childId.isNullOrEmpty()) {
                val selectedChildIndex = childList.indexOfFirst { it.id == childId }
                if (selectedChildIndex != -1) {
                    child_list.setSelection(selectedChildIndex)
                }
            }

            // Set listener for spinner selection changes
            child_list.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedChildId = childList[position]
                    val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("child_id", selectedChildId.id)
                    editor.apply()

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Handle case where nothing is selected if needed
                }
            }

        }
    }

    private fun updateAllCards(childId: String) {
        // Reference to the Child document
        val childDocRef = db.collection("Child").document(childId)

        // Fetch screen_time and location (GeoPoint)
        childDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    // Get screen_time as a String
                    val screenTime = documentSnapshot.getString("screen_time")
                    // Set screen_time to the TextView (time_value_txt)
                    time_value_txt.text = screenTime ?: "N/A"

                    // Get location as a GeoPoint
                    val geoPoint = documentSnapshot.getGeoPoint("location")
                    if (geoPoint != null) {
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        val geoApiContext = GeoApiContext.Builder()
                            .apiKey("AIzaSyD70Xp36MbJ3aREpZYDHmcbgAXar4-c4SM")
                            .build()

                        try {
                            val results: Array<GeocodingResult> = GeocodingApi.reverseGeocode(geoApiContext, latLng).await()
                            if (results.isNotEmpty()) {
                                val placeName = results[0].formattedAddress
                                place_txt.text = placeName
                            } else {
                                place_txt.text = "Unknown Location"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            place_txt.text = "Error fetching location"
                        }
                    } else {
                        place_txt.text = "No Location"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch child document: $e")
            }

        // Fetch the number of documents in the App_time_limits collection
        childDocRef.collection("App_time_limits")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val limitCount = querySnapshot.size()
                // Set the document count as "x apps" in the TextView (limit_value_txt)
                limit_value_txt.text = "$limitCount apps"
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch App_time_limits collection: $e")
            }
    }



    private fun storeChildIdInSharedPrefs(childId: String, childIds: List<String>) {
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val childIdsJson = Gson().toJson(childIds)

        editor.putString("child_id", childId)
        editor.putString("child_ids", childIdsJson)
        editor.apply()
    }

    fun addChildDevice() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.scan_dialog) // Reference to your dialog layout XML
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        // Find views and set click listeners in dialog
        val pairButton = dialog.findViewById<AppCompatButton>(R.id.pair_btn)
        pairButton.setOnClickListener {
            // Logic for scanning and pairing
            startscanning(auth!!.uid)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun startscanning(userid: String) {
        scanner.startScan().addOnSuccessListener { scanResult ->
            val rawValue = scanResult.rawValue

            rawValue?.let { result ->
                // Ensure that the result is not a URL or contains invalid characters
                if (!result.contains("http") && !result.contains("//")) {
                    // Step 1: Check if the child document exists in the Child collection
                    db.collection("Child")
                        .document(result)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                // Step 2: If the childId exists, update the Parent's child_ids array
                                db.collection("Parent")
                                    .document(userid)
                                    .update("child_ids", FieldValue.arrayUnion(result))
                                    .addOnSuccessListener {
                                        storedChildIds.add(result)
                                        val updatedChildIdsJson = Gson().toJson(storedChildIds)
                                        val editor = sharedPref.edit()
                                        editor.putString("child_ids", updatedChildIdsJson)
                                        editor.apply()

                                        Toast.makeText(requireContext(), "Child Profile added successfully", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("HomeFragment", "Error updating parent profile: ${e.message}")
                                        Toast.makeText(requireContext(), "Issue found while updating parent profile", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                // Handle case where the scanned result is not a valid child profile
                                Toast.makeText(requireContext(), "Invalid Child Profile", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeFragment", "Error fetching child profile: ${e.message}")
                            Toast.makeText(requireContext(), "Error checking Child Profile", Toast.LENGTH_LONG).show()
                        }
                } else {
                    // Handle case where scanned result is invalid
                    Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnCanceledListener {
            Toast.makeText(requireContext(), "Scan Cancelled", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Log.e("HomeFragment", "Scan error: ${it.message}")
            Toast.makeText(requireContext(), "Issue found during scan", Toast.LENGTH_LONG).show()
        }

    }

}