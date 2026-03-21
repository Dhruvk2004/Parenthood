package com.example.boardingscreen.presentation.parent.map

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
import androidx.lifecycle.ViewModelProvider
import com.example.boardingscreen.presentation.parent.map.GeofenceData
import com.example.boardingscreen.presentation.parent.map.MapUiState
import com.example.boardingscreen.presentation.parent.map.MapViewModel
import com.example.boardingscreen.presentation.parent.map.MapViewModelFactory
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
import com.example.boardingscreen.R
import com.example.boardingscreen.BuildConfig
import com.example.boardingscreen.adapters.GeofenceListAdapter
import com.example.boardingscreen.data.model.Geofence_details

class MapFragment : Fragment(), OnMapReadyCallback {

    private var gmap: GoogleMap? = null
    private lateinit var option: ImageView
    private lateinit var curr_location: ImageView
    private val markersList = mutableListOf<Marker>()
    private val circlesList = mutableListOf<Circle>()
    private var selectedLatLng: LatLng? = null
    private var selectedPlaceName: String? = null
    
    // For geofence preview
    private var previewMarker: Marker? = null
    private var previewCircle: Circle? = null
    private var currentDialog: Dialog? = null

    private lateinit var viewModel: MapViewModel

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

        initializeViews(view)
        initializeViewModel()
        observeUiState()

        option.setOnClickListener {
            Log.d("MapFragment", "Options menu clicked")
            OpenAndHandleOptionsMenu(it)
        }

        curr_location.setOnClickListener {
            viewModel.loadChildLocation()
        }
    }

    private fun initializeViews(view: View) {
        option = view.findViewById(R.id.map_options)
        curr_location = view.findViewById(R.id.curr_location)
    }

    private fun initializeViewModel() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val factory = MapViewModelFactory(requireContext(), sharedPref)
        viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MapUiState.Loading -> showLoadingState()
                is MapUiState.NoInternet -> showNoInternetState()
                is MapUiState.NoChild -> showNoChildState()
                is MapUiState.ChildLocationLoaded -> showChildLocation(state.location, state.geofences)
                is MapUiState.GeofenceAdded -> handleGeofenceAdded(state.geofence)
                is MapUiState.GeofenceDeleted -> handleGeofenceDeleted()
                is MapUiState.Error -> showErrorState(state.message)
            }
        }
    }

    private fun showLoadingState() {
        Toast.makeText(requireContext(), "Loading...", Toast.LENGTH_SHORT).show()
    }

    private fun showNoInternetState() {
        Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_LONG).show()
    }

    private fun showNoChildState() {
        option.visibility = View.GONE
        curr_location.visibility = View.GONE
        Toast.makeText(requireContext(), "No Child Device is connected", Toast.LENGTH_LONG).show()
    }

    private fun showChildLocation(location: LatLng, geofences: List<GeofenceData>) {
        clearMap()
        option.visibility = View.VISIBLE
        curr_location.visibility = View.VISIBLE

        // Add child location marker
        val markerOp = MarkerOptions()
            .position(location)
            .title("Child's Device Location")
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.curr_location)!!))
        val marker = gmap?.addMarker(markerOp)
        marker?.let { markersList.add(it) }

        // Animate camera to child location
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))

        // Add all geofences
        for (geofence in geofences) {
            add_circle(geofence.location, geofence.radius, geofence.name)
        }
    }

    private fun handleGeofenceAdded(geofence: GeofenceData) {
        Toast.makeText(requireContext(), "Geofence saved successfully", Toast.LENGTH_SHORT).show()
        // Just add the circle without zooming - user can use curr_location button to go back
        add_circle(geofence.location, geofence.radius, geofence.name)
    }

    private fun handleGeofenceDeleted() {
        Toast.makeText(requireContext(), "Geofence removed successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorState(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.gmap = googleMap
        gmap?.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
        gmap?.uiSettings?.isZoomControlsEnabled = false
        gmap?.uiSettings?.isCompassEnabled = true
        gmap?.uiSettings?.isMapToolbarEnabled = false

        // Load child location
        viewModel.loadChildLocation()
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

    private fun show_places() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView: View = LayoutInflater.from(requireContext()).inflate(R.layout.show_geofence_bottom_sheet, null)
        val geo_list = bottomSheetView.findViewById<ListView>(R.id.geofence_listview)

        viewModel.getGeofencesList(
            onSuccess = { geofenceDataList ->
                if (geofenceDataList.isEmpty()) {
                    Toast.makeText(requireContext(), "No geofence details found", Toast.LENGTH_SHORT).show()
                } else {
                    // Convert GeofenceData to Geofence_details for adapter
                    val geofenceList = geofenceDataList.map {
                        Geofence_details(it.geofenceId, it.name, it.placeName, it.radius)
                    } as ArrayList<Geofence_details>

                    val listAdapter = GeofenceListAdapter(requireContext(), geofenceList, true) { geofence ->
                        deleteGeofence(geofence, geofenceList, bottomSheetDialog)
                    }
                    geo_list.adapter = listAdapter
                }
            },
            onError = { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        )

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun deleteGeofence(
        geofence: Geofence_details,
        geofenceList: ArrayList<Geofence_details>,
        bottomSheetDialog: BottomSheetDialog
    ) {
        geofenceList.remove(geofence)
        viewModel.deleteGeofence(geofence.geofence_id)
        
        if (geofenceList.isEmpty()) {
            bottomSheetDialog.dismiss()
        }
    }


    private fun showAddGeofenceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.add_geofence_dialog, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        currentDialog = dialog
        
        dialog.setOnShowListener {
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
            }

            Handler(Looper.getMainLooper()).post {
                initializeAutocompleteFragment(childFragmentManager)
            }
        }

        val placeName = dialog.findViewById<TextInputEditText>(R.id.name_txt)
        val seekbarRadius = dialog.findViewById<SeekBar>(R.id.radius_seekbar)
        val textviewRadiusValue = dialog.findViewById<TextView>(R.id.rad_txt)
        val buttonAdd = dialog.findViewById<Button>(R.id.save_btn)
        val nameLayout = dialog.findViewById<View>(R.id.name_layout)
        val autoCard = dialog.findViewById<View>(R.id.auto_card)
        
        // Get the root CardView to hide entire dialog
        val rootCard = dialogView as? androidx.cardview.widget.CardView

        seekbarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textviewRadiusValue.text = "Radius: ${progress}m"
                // Update preview circle in real-time
                if (selectedLatLng != null) {
                    updatePreviewCircle(selectedLatLng!!, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Hide entire dialog card except seekbar area
                if (selectedLatLng != null) {
                    rootCard?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                    rootCard?.cardElevation = 0f
                    nameLayout.visibility = View.INVISIBLE
                    autoCard.visibility = View.INVISIBLE
                    buttonAdd.visibility = View.INVISIBLE
                    dialog.window?.setDimAmount(0f)
                }
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Show everything again
                rootCard?.setCardBackgroundColor(android.graphics.Color.WHITE)
                rootCard?.cardElevation = 12f
                nameLayout.visibility = View.VISIBLE
                autoCard.visibility = View.VISIBLE
                buttonAdd.visibility = View.VISIBLE
                dialog.window?.setDimAmount(0.5f)
            }
        })

        buttonAdd.setOnClickListener {
            val radius = seekbarRadius.progress

            if (selectedPlaceName != null && selectedLatLng != null && radius != 0 && placeName.text?.length != 0) {
                // Remove preview before adding actual geofence
                clearPreview()
                viewModel.addGeofence(
                    placeName.text.toString(),
                    selectedPlaceName!!,
                    selectedLatLng!!,
                    radius
                )
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Invalid geofence", Toast.LENGTH_LONG).show()
            }
        }

        dialog.setOnDismissListener {
            val fragment = childFragmentManager.findFragmentById(R.id.autoCompleteFragment)
            if (fragment != null) {
                childFragmentManager.beginTransaction().remove(fragment).commit()
            }
            clearPreview()
            selectedPlaceName = null
            selectedLatLng = null
            currentDialog = null
        }

        dialog.show()
    }
    
    private fun updatePreviewCircle(location: LatLng, radius: Int) {
        // Remove existing preview circle
        previewCircle?.remove()
        
        // Draw new preview circle
        val circleOptions = CircleOptions()
            .center(location)
            .radius(radius.toDouble())
            .strokeColor(ResourcesCompat.getColor(resources, R.color.dark_blue, null))
            .fillColor(ResourcesCompat.getColor(resources, R.color.geofence_fill, null))
            .strokeWidth(4f)
        
        previewCircle = gmap?.addCircle(circleOptions)
        
        // Adjust zoom based on radius
        val zoomLevel = getZoomLevel(radius)
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
    }
    
    private fun showPreviewMarker(location: LatLng) {
        // Remove existing preview marker
        previewMarker?.remove()
        
        // Add preview marker
        val markerOptions = MarkerOptions()
            .position(location)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.geo_location)!!))
            .title("New Geofence")
        
        previewMarker = gmap?.addMarker(markerOptions)
        
        // Move camera to location
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }
    
    private fun clearPreview() {
        previewMarker?.remove()
        previewMarker = null
        previewCircle?.remove()
        previewCircle = null
    }

    private fun initializeAutocompleteFragment(fragmentManager: FragmentManager) {
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
                Log.d("Autocompleteview", status.statusMessage.toString())
            }

            override fun onPlaceSelected(place: Place) {
                selectedPlaceName = place.name
                selectedLatLng = place.latLng
                
                // Show preview marker and move map to selected location
                place.latLng?.let { latLng ->
                    showPreviewMarker(latLng)
                }
            }
        })
    }

    private fun add_circle_and_zoom(latlng: LatLng, radius: Int, name: String) {
        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(radius.toDouble())
            .strokeColor(ResourcesCompat.getColor(resources, R.color.dark_blue, null))
            .fillColor(ResourcesCompat.getColor(resources, R.color.geofence_fill, null))
            .strokeWidth(4f)
        
        Log.d("Drawing circle", "Drawing circle of radius: $radius")
        val circle = gmap?.addCircle(circleOptions)
        circle?.let { circlesList.add(it) }

        val markerOptions = MarkerOptions()
            .position(latlng)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.geo_location)!!))
            .title(name)

        val marker = gmap?.addMarker(markerOptions)
        marker?.let { markersList.add(it) }

        val zoomLevel = getZoomLevel(radius)
        gmap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel))

        Log.d("Zoom", "Zooming to latlng: $latlng with zoom level: $zoomLevel")
    }

    private fun getZoomLevel(radius: Int): Float {
        return when {
            radius <= 100 -> 17f
            radius <= 500 -> 15f
            radius <= 1000 -> 14f
            radius <= 5000 -> 12f
            else -> 10f
        }
    }

    private fun clearMap() {
        for (marker in markersList) {
            marker.remove()
        }
        markersList.clear()

        for (circle in circlesList) {
            circle.remove()
        }
        circlesList.clear()
    }

    private fun add_circle(latlng: LatLng, radius: Int, geo_name: String) {
        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(radius.toDouble())
            .strokeColor(ResourcesCompat.getColor(resources, R.color.dark_blue, null))
            .fillColor(ResourcesCompat.getColor(resources, R.color.geofence_fill, null))
            .strokeWidth(4f)
        
        val circle = gmap?.addCircle(circleOptions)
        circle?.let { circlesList.add(it) }

        val markerOptions = MarkerOptions()
            .position(latlng)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromDrawable(R.drawable.geo_location)!!))
            .title(geo_name)

        val marker = gmap?.addMarker(markerOptions)
        marker?.let { markersList.add(it) }
    }

    private fun getBitmapFromDrawable(resid: Int): Bitmap? {
        var bitmap: Bitmap? = null
        val drawable = ResourcesCompat.getDrawable(resources, resid, null)
        if (drawable != null) {
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
        return bitmap
    }
}