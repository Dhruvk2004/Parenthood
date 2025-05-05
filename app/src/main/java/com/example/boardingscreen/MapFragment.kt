package com.example.boardingscreen

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class MapFragment : Fragment(), OnMapReadyCallback {

    private var gmap: GoogleMap? = null
    lateinit var option:ImageView
    var child_id:String? = null
    var parent_id=""
    lateinit var curr_location:ImageView
    private var auth = FirebaseAuth.getInstance().currentUser
    private var db = FirebaseFirestore.getInstance()
    private val markersList = mutableListOf<Marker>()
    private val circlesList = mutableListOf<Circle>()
    private lateinit var ListAdapter: ListAdapter
    var selectedLatLng: LatLng? = null
    var selectedPlaceName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        parent_id=auth!!.uid
        option=view.findViewById(R.id.map_options)
        curr_location=view.findViewById(R.id.curr_location)

        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        child_id = sharedPref.getString("child_id", null)

        option.setOnClickListener {
            Log.d("MapFragment", "Options menu clicked")
            OpenAndHandleOptionsMenu(it)
        }

        curr_location.setOnClickListener {
            MoveToChildLocation()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.gmap = googleMap
        gmap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        gmap?.uiSettings?.isZoomControlsEnabled = false
        gmap?.uiSettings?.isCompassEnabled = true
        gmap?.uiSettings?.isMapToolbarEnabled = false

        if(child_id=="" || child_id==null){
            Toast.makeText(requireContext(),"No Child Device is connected",Toast.LENGTH_LONG).show()
        }else{

            MoveToChildLocation()
        }
    }

    private fun OpenAndHandleOptionsMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.geo_options)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.show_geofences -> {
                    show_places()
                    true
                }
                R.id.add_geofence -> {
                    // Toast.makeText(this,"Open Dialog box with options",Toast.LENGTH_LONG).show()
                    showAddGeofenceDialog()
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    fun show_places(){
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        // Inflate the custom layout for the bottom sheet
        val bottomSheetView: View = LayoutInflater.from(requireContext()).inflate(R.layout.show_geofence_bottom_sheet, null)

        // Find the ImageView for the QR code and set your QR code image
        val geo_list = bottomSheetView.findViewById<ListView>(R.id.geofence_listview)

        val geofenceList= arrayListOf<Geofence_details>()
        // Reference to the 'geo_details' subcollection inside the child's document
        // Use childId to retrieve geofence details
        db.collection("Child").document(child_id!!)
            .collection("geo_details")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val geo_name = document.getString("Name")
                        val placeName = document.getString("place")
                        val radius = document.getDouble("radius")?.toInt()
                        val geo_id = document.id


                        if(geo_id != null && geo_name != null && radius != null && placeName != null){
                            val geofenceDetail = Geofence_details(geo_id, geo_name, placeName, radius)
                            geofenceList.add(geofenceDetail)
                        }
                    }

                    // Set up the adapter
                    ListAdapter = ListAdapter(requireContext(), geofenceList, true) { geofence ->
                        deleteGeofence(geofence, geofenceList,ListAdapter)
                    }
                    geo_list.adapter = ListAdapter
                } else {
                    Toast.makeText(requireContext(), "No geofence details found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to retrieve geofence details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        // Show the bottom sheet
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun deleteGeofence(geofence: Geofence_details, geofenceList: ArrayList<Geofence_details>, geofenceAdapter: ListAdapter) {
        // Remove geofence from list
        geofenceList.remove(geofence)
        RemoveGeofenceFromFirestore(geofence.geofence_id)
        // Notify adapter about the change
        geofenceAdapter.notifyDataSetChanged()
        MoveToChildLocation()
    }

    private fun RemoveGeofenceFromFirestore(geofenceId: String) {
        // Reference to the 'geo_details' subcollection inside the child's document
        db.collection("Child").document(child_id!!)
            .collection("geo_details").document(geofenceId)
            .delete()
            .addOnSuccessListener {
                // Successfully deleted the geofence from Firestore
                Toast.makeText(requireContext(), "Geofence removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                // Failed to delete the geofence
                Toast.makeText(requireContext(), "Failed to remove geofence: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    fun showAddGeofenceDialog() {
        // Inflate the dialog_add_geofence.xml layout
        val dialogView = layoutInflater.inflate(R.layout.add_geofence_dialog, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background
        dialog.setOnShowListener {
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), getString(R.string.google_maps_api_key))
            }

            Handler(Looper.getMainLooper()).post{
                initializeAutocompleteFragment(childFragmentManager)
            }
        }

        // Access the SeekBar, TextView, and Button
        val placeName = dialog.findViewById<TextInputEditText>(R.id.name_txt)
        val seekbarRadius = dialog.findViewById<SeekBar>(R.id.radius_seekbar)
        val textviewRadiusValue = dialog.findViewById<TextView>(R.id.rad_txt)
        val buttonAdd = dialog.findViewById<Button>(R.id.save_btn)


        // Update TextView with SeekBar value in real-time
        seekbarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textviewRadiusValue.text = "Radius: ${progress}m"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Not needed
            }
        })

        // Set up button click listener
        buttonAdd.setOnClickListener {
            val radius = seekbarRadius.progress

            if (selectedPlaceName != null && selectedLatLng != null && radius != 0 && placeName.text?.length != 0) {
               store_geofence_to_firebase(placeName.text.toString(), selectedPlaceName!!, selectedLatLng!!, radius)
                add_circle_and_zoom(selectedLatLng!!, radius, placeName.text.toString())
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Invalid geofence", Toast.LENGTH_LONG).show()
            }
        }

        // Set up dialog dismiss to remove the Autocomplete fragment
        dialog.setOnDismissListener {
            val fragment = childFragmentManager.findFragmentById(R.id.autoCompleteFragment)
            if (fragment != null) {
                childFragmentManager.beginTransaction().remove(fragment).commit()
            }
            selectedPlaceName = null
            selectedLatLng = null
        }

        dialog.show()
    }

    fun store_geofence_to_firebase(name: String,selectedPlaceName:String, selectedLatLng: LatLng,radius: Int){
        //logic to store geofence to firebase
        val geoPoint = GeoPoint(selectedLatLng.latitude, selectedLatLng.longitude)
        val geoDetails = hashMapOf(
            "Name" to name,
            "place" to selectedPlaceName,
            "geopoint" to geoPoint,
            "radius" to radius
        )

        db.collection("Child").document(child_id!!)
            .collection("geo_details").add(geoDetails)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Geofence saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to save geofence details", Toast.LENGTH_SHORT).show()
            }
    }

    fun initializeAutocompleteFragment(fragmentManager: FragmentManager){
        var autocompleteFragment = fragmentManager.findFragmentById(R.id.autoCompleteFragment) as? AutocompleteSupportFragment
        if (autocompleteFragment == null) {
            autocompleteFragment = AutocompleteSupportFragment().apply {
                fragmentManager.beginTransaction()
                    .replace(R.id.autoCompleteFragment, this)
                    .commitNow()
            }
        }

        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(status: Status) {
                Log.d("Autocompleteview",status.statusMessage.toString())
            }

            override fun onPlaceSelected(place: Place) {
                selectedPlaceName = place.name
                selectedLatLng = place.latLng
            }
        })
    }

    fun add_circle_and_zoom(latlng: LatLng,radius: Int, name:String){
        // Create a circle on the map

        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(radius.toDouble()) // Convert radius to double for circle radius
            .strokeColor(ResourcesCompat.getColor(resources, R.color.dark_blue, null)) // Set circle stroke color
            .fillColor(ResourcesCompat.getColor(resources, R.color.geofence_fill, null))
            .strokeWidth(4f) // Set circle fill color with transparency
        Log.d("Drawing cirlce", "Drawing circle of radius: $radius")
        val circle= gmap?.addCircle(circleOptions)
        circle?.let { circlesList.add(it) }

        // Add a marker at the circle's center with a location icon
        val markerOptions = MarkerOptions()
            .position(latlng)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.geo_location)!!)) // Set custom marker icon
            .title(name)

        val marker = gmap?.addMarker(markerOptions)
        marker?.let { markersList.add(it) }

        // Animate the camera to zoom into the circle's location
        val zoomLevel = getZoomLevel(radius)
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel))

        Log.d("Zoom", "Zooming to latlng: $latlng with zoom level: $zoomLevel")
    }

    // Helper function to calculate zoom level based on radius
    fun getZoomLevel(radius: Int): Float {
        return when {
            radius <= 100 -> 17f
            radius <= 500 -> 15f
            radius <= 1000 -> 14f
            radius <= 5000 -> 12f
            else -> 10f
        }
    }

    fun clearMap(){
        // Remove all markers from the map
        for (marker in markersList) {
            marker.remove()
        }
        markersList.clear()

        // Remove all circles from the map
        for (circle in circlesList) {
            circle.remove()
        }
        circlesList.clear()
    }

    private fun MoveToChildLocation(){
        clearMap()
        if (child_id != null && child_id!!.isNotEmpty()) {
            // Fetch the child's location directly using the childId
            db.collection("Child").document(child_id!!).get()
                .addOnSuccessListener { childDocument ->
                    if (childDocument != null && childDocument.exists()) {
                        val location = childDocument.getGeoPoint("location")
                        if (location != null) {
                            option.visibility = View.VISIBLE
                            curr_location.visibility = View.VISIBLE
                            val latLng = LatLng(location.latitude, location.longitude)
                            gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15F))
                            val markerOp = MarkerOptions()
                                .position(latLng)
                                .title("Child's Device Location")
                                .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.curr_location)!!))
                            val marker = gmap?.addMarker(markerOp)
                            marker?.let { markersList.add(it) }
                        } else {
                            Toast.makeText(requireContext(), "Location data not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "No such document!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to retrieve location: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

            mark_all_geofences()
        } else {
            Toast.makeText(requireContext(), "Child ID is null or empty", Toast.LENGTH_SHORT).show()
        }

    }

    fun mark_all_geofences() {
        // Use childId to retrieve geofence details
        db.collection("Child").document(child_id!!)
            .collection("geo_details")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val geo_name = document.getString("Name")
                        val geoPoint = document.getGeoPoint("geopoint")
                        val placeName = document.getString("place")
                        val radius = document.getDouble("radius")?.toInt()

                        if (geoPoint != null && radius != null && geo_name != null) {
                            val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                            add_circle(latLng, radius, geo_name)
                            // Log or display the place name for reference
                            Log.d("GeofenceDetails", "Place: $placeName, LatLng: $latLng, Radius: $radius")
                        }
                    }
                } else {
                    //Toast.makeText(requireContext(), "No geofence details found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to retrieve geofence details: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun add_circle(latlng: LatLng,radius: Int,geo_name: String){
        // Create a circle on the map
        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(radius.toDouble()) // Convert radius to double for circle radius
            .strokeColor(ResourcesCompat.getColor(resources, R.color.dark_blue, null)) // Set circle stroke color
            .fillColor(ResourcesCompat.getColor(resources, R.color.geofence_fill, null))
            .strokeWidth(4f) // Set circle fill color with transparency
       val circle= gmap?.addCircle(circleOptions)
        circle?.let { circlesList.add(it) }

        // Add lock marker to represent geofence
        val markerOptions = MarkerOptions()
            .position(latlng)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.geo_location)!!)) // Use lock icon drawable
            .title(geo_name)

        val marker = gmap?.addMarker(markerOptions)
        marker?.let { markersList.add(it) }
    }


    private fun getBitmapFromDrawable(resid: Int): Bitmap? {
        var bitmap: Bitmap?=null
        val drawable = ResourcesCompat.getDrawable(resources, resid, null)
        if (drawable != null){
            bitmap = Bitmap.createBitmap(100,100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0,0,canvas.width,canvas.height)
            drawable.draw(canvas)
        }
        return bitmap
    }

}