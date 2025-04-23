package com.example.ryd

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.get

class HomeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "HomeActivity"
        private const val FROM_LOCATION_REQUEST_CODE = 100
        private const val DESTINATION_LOCATION_REQUEST_CODE = 101
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var fabAddRide: FloatingActionButton
    private lateinit var tvUsername: TextView
    private lateinit var tvUserDepartment: TextView
    private lateinit var etFrom: TextInputEditText
    private lateinit var etDestination: TextInputEditText
    private lateinit var etDepartureTime: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var chipDriver: Chip
    private lateinit var chipPassenger: Chip
    private lateinit var btnCreateRide: Button
    private lateinit var rvAvailableRides: RecyclerView
    private lateinit var noRidesLayout: LinearLayout

    // Data
    private lateinit var rideAdapter: RideAdapter
    private val ridesList = mutableListOf<Ride>()
    private var selectedDate: Long = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val descriptionInputLayout = findViewById<TextInputLayout>(R.id.descriptionInputLayout)
        val btnPostRide = findViewById<MaterialButton>(R.id.btnPostRide)
        val btnFindMatches = findViewById<MaterialButton>(R.id.btnFindMatches)



        descriptionInputLayout.visibility = View.GONE

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeUI()

        // Set up AuthStateListener
        setupAuthStateListener()

        // Set up click listeners
        setupClickListeners()
        setupLocationPickers()

        // Load user data
        loadUserInfo()

        // Load available rides
        loadAvailableRides()

        btnPostRide.setOnClickListener {
            // If description is hidden, show it and change button text
            if (descriptionInputLayout.visibility == View.GONE) {
                descriptionInputLayout.visibility = View.VISIBLE
                btnPostRide.text = "Confirm Post"

                // Smooth scroll to show the description field
                val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
                nestedScrollView?.post {
                    nestedScrollView.smoothScrollTo(0, descriptionInputLayout.top)
                }
            } else {
                // Description is already showing, proceed with creating the ride
                createRide()
            }
        }

        btnFindMatches.setOnClickListener {
            // Hide description if it's visible
            if (descriptionInputLayout.visibility == View.VISIBLE) {
                descriptionInputLayout.visibility = View.GONE
                btnPostRide.text = "Post Ride"
            }

            // Use our new advanced matching function
            findMatches()
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun initializeUI() {
        // Find views
        toolbar = findViewById(R.id.toolbar)
        bottomNavView = findViewById(R.id.bottomNavView)
        fabAddRide = findViewById(R.id.fabAddRide)
        tvUsername = findViewById(R.id.tvUsername)
        tvUserDepartment = findViewById(R.id.tvUserDepartment)
        etFrom = findViewById(R.id.etFrom)
        etDestination = findViewById(R.id.etDestination)
        etDescription = findViewById(R.id.etDescription)
        etDepartureTime = findViewById(R.id.etDepartureTime)
        chipDriver = findViewById(R.id.chipDriver)
        chipPassenger = findViewById(R.id.chipPassenger)
        btnCreateRide = findViewById(R.id.btnFindMatches)
        rvAvailableRides = findViewById(R.id.rvAvailableRides)
        noRidesLayout = findViewById(R.id.noRidesLayout)

        // Set up toolbar
        setSupportActionBar(toolbar)

        // Set up RecyclerView
        rvAvailableRides.layoutManager = LinearLayoutManager(this)
        rideAdapter = RideAdapter(ridesList, this::onRideSelected)
        rvAvailableRides.adapter = rideAdapter

        // Set up bottom navigation
        bottomNavView.setOnItemSelectedListener { item ->
            handleBottomNavigation(item)
        }

        // Make departure time read-only (will be set via date/time pickers)
        etDepartureTime.isFocusable = false
        etDepartureTime.isClickable = true
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // User signed out, redirect to login
                Log.d(TAG, "User signed out, redirecting to login")
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupClickListeners() {
        // FAB click listener
        fabAddRide.setOnClickListener {
            // Scroll to the create ride section
            (findViewById<View>(R.id.createRideCard)).requestFocus()
            // Or alternatively show a full screen dialog for ride creation
        }

        // Departure time click listener
        etDepartureTime.setOnClickListener {
            showDateTimePicker()
        }

        // Create ride button click listener
        btnCreateRide.setOnClickListener {
            createRide()
        }

        // Chip group logic for driver/passenger selection
        chipDriver.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                chipPassenger.isChecked = false
//                etSeats.visibility = View.VISIBLE
            } else if (!chipPassenger.isChecked) {
                chip.isChecked = true // Ensure at least one is selected
            }
        }

        chipPassenger.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                chipDriver.isChecked = false
//                etSeats.visibility = View.GONE
            } else if (!chipDriver.isChecked) {
                chip.isChecked = true // Ensure at least one is selected
            }
        }
    }

    private fun loadAvailableRides() {
        val currentUser = auth.currentUser ?: return

        // Clear existing rides
        ridesList.clear()

        // Query rides from Firestore
        firestore.collection("rides")
            .whereGreaterThan("departureTime", System.currentTimeMillis())
            .orderBy("departureTime", Query.Direction.ASCENDING)
            .limit(20) // Limit to 20 rides
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Parse ride data
                    val ride = document.toObject(Ride::class.java).apply {
                        id = document.id
                    }

                    // Don't add user's own rides to the available list
                    if (ride.userId != currentUser.uid) {
                        ridesList.add(ride)
                    }
                }

                // Update UI
                updateRidesUI()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error loading rides", e)
                Toast.makeText(this, "Failed to load rides: ${e.message}", Toast.LENGTH_SHORT).show()
                updateRidesUI()
            }
    }

    private fun updateRidesUI() {
        if (ridesList.isEmpty()) {
            rvAvailableRides.visibility = View.GONE
            noRidesLayout.visibility = View.VISIBLE
        } else {
            rvAvailableRides.visibility = View.VISIBLE
            noRidesLayout.visibility = View.GONE
            rideAdapter.notifyDataSetChanged()
        }
    }

    private fun showDateTimePicker() {
        // Show date picker first
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select departure date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { dateInMillis ->
            selectedDate = dateInMillis

            // Now show time picker
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select departure time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute

                // Combine date and time
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selectedDate
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)

                // Format the selected date and time
                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                etDepartureTime.setText(dateFormat.format(calendar.time))
            }

            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun createRide() {
        val currentUser = auth.currentUser ?: return

        // Validate inputs
        val fromLocation = etFrom.text.toString().trim()
        if (fromLocation.isEmpty()) {
            etFrom.error = "Please enter a starting location"
            return
        }

        val destination = etDestination.text.toString().trim()
        if (destination.isEmpty()) {
            etDestination.error = "Please enter a destination"
            return
        }

        if (selectedDate == 0L) {
            etDepartureTime.error = "Please select departure time"
            return
        }

        // Combine date and time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        calendar.set(Calendar.MINUTE, selectedMinute)
        val departureTime = calendar.timeInMillis

        // Check if departure time is in the future
        if (departureTime <= System.currentTimeMillis()) {
            etDepartureTime.error = "Departure time must be in the future"
            return
        }

        // Get ride type and seats
        val isDriver = chipDriver.isChecked
        val description = findViewById<TextInputEditText>(R.id.etDescription).text.toString().trim()

        if (isDriver) {
//            val seatsText = etSeats.text.toString()
//            if (seatsText.isEmpty()) {
//                etSeats.error = "Please enter available seats"
//                return
//            }
//            seats = seatsText.toIntOrNull() ?: 0
//            if (seats <= 0) {
//                etSeats.error = "Must have at least 1 seat"
//                return
//            }
        }

        // Create ride object
        val ride = Ride(
            id = "",
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "Anonymous",
            userPhoto = currentUser.photoUrl?.toString() ?: "",
            fromLocation = fromLocation,
            destination = destination,
            departureTime = departureTime,
            isDriver = isDriver,
            seats = 1,
            description = description,
            timestamp = System.currentTimeMillis()
        )

        // Save to Firestore
        firestore.collection("rides")
            .add(ride)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Ride created with ID: ${documentReference.id}")
                Toast.makeText(this, "Ride created successfully!", Toast.LENGTH_SHORT).show()

                // Clear inputs
                etFrom.text?.clear()
                etDestination.text?.clear()
                etDepartureTime.text?.clear()
                etDescription.text?.clear()
                selectedDate = 0

                // Refresh available rides
                loadAvailableRides()

                // Show find matches dialog
                showMatchesDialog()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding ride", e)
                Toast.makeText(this, "Failed to create ride: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMatchesDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Find Matches")
            .setMessage("Would you like to see potential matches for your ride?")
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, MatchesActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun onRideSelected(ride: Ride) {
        // Open ride details or request dialog
        val intent = Intent(this, RideDetailActivity::class.java)
        intent.putExtra("rideId", ride.id)
        startActivity(intent)
    }

    private fun handleBottomNavigation(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_home -> {
                // Already on home, do nothing
                true
            }
            R.id.nav_my_rides -> {
                startActivity(Intent(this, MyRidesActivity::class.java))
                true
            }
            R.id.nav_messages -> {
                startActivity(Intent(this, MessagesActivity::class.java))
                true
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> false
        }
    }

    // Sign out method
    private fun signOut() {
        auth.signOut()
        // AuthStateListener will handle the redirect
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        // Reset bottom navigation selection to Home when returning to this activity
        bottomNavView.selectedItemId = R.id.nav_home

        // Refresh data if needed
//        loadUserData()
//        loadAvailableRides()
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Get user's name for the "Hi, [name]" greeting
                    val userName = document.getString("name") ?: currentUser.displayName ?: "User"
                    val greeting = findViewById<TextView>(R.id.tvUsername)
                    greeting.text = "Hi, $userName"

                    // Extract academic year and branch
                    val academicYear = document.getLong("academicYear")?.toInt() ?: 0
                    val branch = document.getString("branch") ?: "Unknown"

                    // Format the year text
                    val yearText = when (academicYear) {
                        1 -> "1st year"
                        2 -> "2nd year"
                        3 -> "3rd year"
                        4 -> "4th year"
                        else -> "$academicYear year"
                    }

                    // Update the subtitle text with academic info instead of "Uni student"
                    val subtitle = findViewById<TextView>(R.id.tvUserDepartment)
                    subtitle.text = "$yearText, $branch"
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting user info", exception)
            }
    }

    private fun setupLocationPickers() {
        // Set up click listener for "From" field to open the map
        etFrom.setOnClickListener {
            // Launch map activity for location selection
            val intent = Intent(this, MapPickerActivity::class.java)
            intent.putExtra("REQUEST_TYPE", "FROM_LOCATION")

            // Pass current location text if it exists
            val currentLocation = etFrom.text.toString().trim()
            if (currentLocation.isNotEmpty()) {
                intent.putExtra("CURRENT_LOCATION", currentLocation)
            }

            startActivityForResult(intent, FROM_LOCATION_REQUEST_CODE)
        }

        etDestination.setOnClickListener {
            // Launch map activity for location selection
            val intent = Intent(this, MapPickerActivity::class.java)
            intent.putExtra("REQUEST_TYPE", "DESTINATION_LOCATION")

            // Pass current destination text if it exists
            val currentDestination = etDestination.text.toString().trim()
            if (currentDestination.isNotEmpty()) {
                intent.putExtra("CURRENT_LOCATION", currentDestination)
            }

            startActivityForResult(intent, DESTINATION_LOCATION_REQUEST_CODE)
        }

        // Make the EditText not focusable to prevent keyboard from showing
        etFrom.isFocusable = false
        etFrom.isClickable = true

        etDestination.isFocusable = false
        etDestination.isClickable = true
    }

    // Handle the result from the map picker
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FROM_LOCATION_REQUEST_CODE -> {
                    data?.let {
                        val locationName = it.getStringExtra("LOCATION_NAME") ?: ""
                        etFrom.setText(locationName)
                    }
                }
                DESTINATION_LOCATION_REQUEST_CODE -> {
                    data?.let {
                        val locationName = it.getStringExtra("LOCATION_NAME") ?: ""
                        etDestination.setText(locationName)
                    }
                }
            }
        }
    }
    // Add this method to HomeActivity.kt
    private fun findMatches() {
        // Get current input values
        val fromLocation = etFrom.text.toString().trim()
        val destination = etDestination.text.toString().trim()
        val departureTime = if (selectedDate > 0) {
            // Combine date and time for comparison
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            calendar.timeInMillis
        } else {
            System.currentTimeMillis()
        }

        // Validate inputs
        if (fromLocation.isEmpty()) {
            etFrom.error = "Please enter a starting location"
            return
        }

        if (destination.isEmpty()) {
            etDestination.error = "Please enter a destination"
            return
        }

        // Show loading dialog
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Finding Matches")
            .setMessage("Searching for rides...")
            .setCancelable(false)
            .show()

        // Step 1: Get coordinates for the from location and destination
        getCoordinatesForLocation(fromLocation) { fromCoordinates ->
            if (fromCoordinates == null) {
                loadingDialog.dismiss()
                Toast.makeText(this, "Could not find coordinates for starting location", Toast.LENGTH_SHORT).show()
                return@getCoordinatesForLocation
            }

            getCoordinatesForLocation(destination) { destCoordinates ->
                if (destCoordinates == null) {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Could not find coordinates for destination", Toast.LENGTH_SHORT).show()
                    return@getCoordinatesForLocation
                }

                // Step 2: Query all upcoming rides
                firestore.collection("rides")
                    .whereGreaterThan("departureTime", System.currentTimeMillis())
                    .get()
                    .addOnSuccessListener { documents ->
                        val currentUser = auth.currentUser ?: return@addOnSuccessListener
                        val matchedRides = mutableListOf<RideMatch>()

                        // Process each ride for matching
                        for (document in documents) {
                            // Skip rides created by the current user
                            if (document.getString("userId") == currentUser.uid) {
                                continue
                            }

                            val ride = document.toObject(Ride::class.java).apply {
                                id = document.id
                            }

                            // Get coordinates for ride's from location
                            getCoordinatesForLocation(ride.fromLocation) { rideFromCoords ->
                                if (rideFromCoords != null) {
                                    // Get coordinates for ride's destination
                                    getCoordinatesForLocation(ride.destination) { rideDestCoords ->
                                        if (rideDestCoords != null) {
                                            // Calculate match scores and create RideMatch objects
                                            val matchScore = calculateMatchScore(
                                                fromCoordinates, destCoordinates,
                                                rideFromCoords, rideDestCoords,
                                                departureTime, ride.departureTime
                                            )

                                            if (matchScore > 0) {
                                                val matchType = determineMatchType(
                                                    fromCoordinates, destCoordinates,
                                                    rideFromCoords, rideDestCoords
                                                )

                                                matchedRides.add(
                                                    RideMatch(
                                                        ride = ride,
                                                        score = matchScore,
                                                        matchType = matchType
                                                    )
                                                )

                                                // When we've processed the last ride, show results
                                                if (matchedRides.size == documents.size() - 1) {
                                                    loadingDialog.dismiss()
                                                    showMatchResults(matchedRides)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // If no rides found or all processed with no matches
                        if (documents.isEmpty()) {
                            loadingDialog.dismiss()
                            Toast.makeText(
                                this,
                                "No rides found matching your criteria",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Error finding matches: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Geocode address to coordinates
    private fun getCoordinatesForLocation(locationName: String, callback: (GeoPoint?) -> Unit) {
        val geocoder = android.location.Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(locationName, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        callback(GeoPoint(address.latitude, address.longitude))
                    } else {
                        callback(null)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(locationName, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    callback(GeoPoint(address.latitude, address.longitude))
                } else {
                    callback(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error: ${e.message}")
            callback(null)
        }
    }

    // Calculate match score (0-100) based on proximity and time
    private fun calculateMatchScore(
        fromCoords: GeoPoint, destCoords: GeoPoint,
        rideFromCoords: GeoPoint, rideDestCoords: GeoPoint,
        requestedTime: Long, rideTime: Long
    ): Int {
        // Distance match (0-50 points)
        val fromDistance = calculateDistance(fromCoords, rideFromCoords)
        val destDistance = calculateDistance(destCoords, rideDestCoords)

        // Give more points for closer origins and destinations
        val maxFromDistance = 5.0 // 5 km max distance for starting point
        val maxDestDistance = 5.0 // 5 km max distance for destination

        val fromScore = (((maxFromDistance - Math.min(fromDistance, maxFromDistance)) / maxFromDistance) * 25).toInt()
        val destScore = (((maxDestDistance - Math.min(destDistance, maxDestDistance)) / maxDestDistance) * 25).toInt()

        // Time match (0-50 points)
        val timeDifferenceHours = Math.abs(requestedTime - rideTime) / (1000.0 * 60 * 60)
        val timeScore = if (timeDifferenceHours <= 24) {
            ((24 - timeDifferenceHours) / 24 * 50).toInt()
        } else {
            0
        }

        return fromScore + destScore + timeScore
    }

    // Compute distance between two points in kilometers using Haversine formula
    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon/2) * Math.sin(dLon/2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))

        return earthRadius * c
    }

    // Determine what type of match this is
    private fun determineMatchType(
        fromCoords: GeoPoint, destCoords: GeoPoint,
        rideFromCoords: GeoPoint, rideDestCoords: GeoPoint
    ): String {
        val fromDistance = calculateDistance(fromCoords, rideFromCoords)
        val destDistance = calculateDistance(destCoords, rideDestCoords)

        // Check if the ride passes near the user's destination
        val isOnRoute = isPointOnRoute(fromCoords, rideFromCoords, rideDestCoords)

        return when {
            fromDistance < 1.0 && destDistance < 1.0 ->
                "Direct match! Same start and end points"
            fromDistance < 1.0 ->
                "Same pickup point, different destination"
            destDistance < 1.0 ->
                "Different pickup, same destination"
            isOnRoute ->
                "This ride passes near your destination"
            else ->
                "Nearby route"
        }
    }

    // Check if a point is roughly on a route between two other points
    private fun isPointOnRoute(point: GeoPoint, routeStart: GeoPoint, routeEnd: GeoPoint): Boolean {
        // Calculate distances
        val distanceStartToEnd = calculateDistance(routeStart, routeEnd)
        val distanceStartToPoint = calculateDistance(routeStart, point)
        val distancePointToEnd = calculateDistance(point, routeEnd)

        // If the point is on the route, the sum of distances should be close to the total route distance
        val buffer = 1.5 // Allow for some deviation (1.5km)
        return distanceStartToPoint + distancePointToEnd <= distanceStartToEnd + buffer
    }

    // Show results to the user
    private fun showMatchResults(matches: List<RideMatch>) {
        if (matches.isEmpty()) {
            Toast.makeText(
                this,
                "No matches found for your route",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Sort matches by score (highest first)
        val sortedMatches = matches.sortedByDescending { it.score }

        // Create an intent to show match results
        val intent = Intent(this, MatchesActivity::class.java)
        intent.putParcelableArrayListExtra("matches", ArrayList(sortedMatches))
        startActivity(intent)
    }

    // Data class for ride matches
    data class RideMatch(
        val ride: Ride,
        val score: Int,
        val matchType: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readParcelable(Ride::class.java.classLoader)!!,
            parcel.readInt(),
            parcel.readString()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(ride, flags)
            parcel.writeInt(score)
            parcel.writeString(matchType)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<RideMatch> {
            override fun createFromParcel(parcel: Parcel): RideMatch {
                return RideMatch(parcel)
            }

            override fun newArray(size: Int): Array<RideMatch?> {
                return arrayOfNulls(size)
            }
        }
    }
}