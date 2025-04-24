package com.example.ryd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ryd.databinding.ActivityMapPickerBinding
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import android.view.View
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.*


class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var confirmButton: MaterialButton
    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView

    private var selectedLocation: GeoPoint? = null
    private var selectedLocationName: String? = null
    private var requestType: String? = null
    private val PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        setContentView(R.layout.activity_map_picker)

        requestType = intent.getStringExtra("REQUEST_TYPE")
        val viewOnlyMode = intent.getBooleanExtra("VIEW_ONLY_MODE", false)
        val locationName = intent.getStringExtra("LOCATION_NAME")

        // Set up the toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set different title based on which field is being edited
        when (requestType) {
            "DESTINATION_LOCATION" -> supportActionBar?.title = "Select Destination"
            else -> supportActionBar?.title = "Select Starting Point"
        }

        mapView = findViewById(R.id.map)
        searchView = findViewById(R.id.searchView)

        // Set up the confirm button
        confirmButton = findViewById(R.id.btnConfirmLocation)
        confirmButton.setOnClickListener {
            if (selectedLocation != null && selectedLocationName != null) {
                val resultIntent = Intent()
                resultIntent.putExtra("LOCATION_NAME", selectedLocationName)
                resultIntent.putExtra("LOCATION_LAT", selectedLocation?.latitude)
                resultIntent.putExtra("LOCATION_LNG", selectedLocation?.longitude)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }

        if (viewOnlyMode && locationName != null) {
            supportActionBar?.title = "Location Details"
            confirmButton?.visibility = View.GONE
            searchLocation(locationName)
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = ArrayList<String>()

        // Only request WRITE_EXTERNAL_STORAGE for SDK < 29 (Q)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Always check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions granted, proceed with map setup
            setupMapView()
            setupSearchView()
        }
    }

    // In MapPickerActivity.kt, modify setupMapView()
    private fun setupMapView() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)

        val mapController = mapView.controller
        mapController.setZoom(15.0)

        // Check if we have a current location passed from the calling activity
        val currentLocation = intent.getStringExtra("CURRENT_LOCATION")
        if (currentLocation != null && currentLocation.isNotEmpty()) {
            // Try to geocode the location string to get coordinates
            searchInitialLocation(currentLocation)
        } else {
            // Default to Nagpur if no location is specified
            val startPoint = GeoPoint(21.1458, 79.0882)
            mapController.setCenter(startPoint)
        }

        // Set up map click listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectLocation(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))
    }

    private fun searchInitialLocation(locationName: String) {
        val geocoder = android.location.Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(locationName, 1) { addresses ->
                    handleInitialAddresses(addresses)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(locationName, 1)
                handleInitialAddresses(addresses)
            }
        } catch (e: Exception) {
            // If geocoding fails, use default location
            val startPoint = GeoPoint(21.1458, 79.0882)
            mapView.controller.setCenter(startPoint)
        }
    }

    private fun handleInitialAddresses(addresses: List<Address>?) {
        runOnUiThread {
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val geoPoint = GeoPoint(address.latitude, address.longitude)

                // Move the map to the found location
                mapView.controller.setCenter(geoPoint)

                // Select this location to show the marker
                selectLocation(geoPoint)
            } else {
                // If no results, use default location
                val startPoint = GeoPoint(21.1458, 79.0882)
                mapView.controller.setCenter(startPoint)
            }
        }
    }
    private fun selectLocation(geoPoint: GeoPoint) {
        // Clear previous markers
        mapView.overlays.removeAll { it is Marker }

        // Add new marker
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)

        selectedLocation = geoPoint
        getLocationName(geoPoint)

        mapView.invalidate()
    }

    private fun getLocationName(geoPoint: GeoPoint) {
        // Use Android's Geocoder to get the address from coordinates
        val geocoder = android.location.Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(
                    geoPoint.latitude, geoPoint.longitude, 1
                ) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        selectedLocationName = address.getAddressLine(0) ?: "Unknown location"
                    } else {
                        selectedLocationName = "Location at ${geoPoint.latitude}, ${geoPoint.longitude}"
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    selectedLocationName = address.getAddressLine(0) ?: "Unknown location"
                } else {
                    selectedLocationName = "Location at ${geoPoint.latitude}, ${geoPoint.longitude}"
                }
            }
        } catch (e: Exception) {
            selectedLocationName = "Location at ${geoPoint.latitude}, ${geoPoint.longitude}"
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchLocation(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchLocation(query: String) {
        val geocoder = android.location.Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(query, 1) { addresses ->
                    handleAddresses(addresses)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                handleAddresses(addresses)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddresses(addresses: List<Address>?) {
        runOnUiThread {
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val geoPoint = GeoPoint(address.latitude, address.longitude)

                mapView.controller.animateTo(geoPoint)
                selectLocation(geoPoint)
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, setup the map
                setupMapView()
                setupSearchView()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Location permissions are required to use the map",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
}