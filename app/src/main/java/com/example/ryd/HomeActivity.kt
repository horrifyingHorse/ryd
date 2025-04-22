package com.example.ryd

import android.content.Intent
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "HomeActivity"
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
    private lateinit var etDestination: TextInputEditText
    private lateinit var etDepartureTime: TextInputEditText
    private lateinit var etSeats: TextInputEditText
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

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeUI()

        // Set up AuthStateListener
        setupAuthStateListener()

        // Set up click listeners
        setupClickListeners()

        // Load user data
        loadUserData()

        // Load available rides
        loadAvailableRides()
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
        etDestination = findViewById(R.id.etDestination)
        etDepartureTime = findViewById(R.id.etDepartureTime)
        etSeats = findViewById(R.id.etSeats)
        chipDriver = findViewById(R.id.chipDriver)
        chipPassenger = findViewById(R.id.chipPassenger)
        btnCreateRide = findViewById(R.id.btnCreateRide)
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
                etSeats.visibility = View.VISIBLE
            } else if (!chipPassenger.isChecked) {
                chip.isChecked = true // Ensure at least one is selected
            }
        }

        chipPassenger.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                chipDriver.isChecked = false
                etSeats.visibility = View.GONE
            } else if (!chipDriver.isChecked) {
                chip.isChecked = true // Ensure at least one is selected
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Display user email while profile loads
            tvUsername.text = "Hi, ${currentUser.displayName ?: currentUser.email?.substringBefore('@') ?: "User"}"

            // Load user profile from Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val department = document.getString("department")

                        // Update UI with user data
                        if (!name.isNullOrEmpty()) {
                            tvUsername.text = "Hi, $name"
                        }
                        if (!department.isNullOrEmpty()) {
                            tvUserDepartment.text = department
                        } else {
                            tvUserDepartment.text = "University Student"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error loading user data", e)
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
        var seats = 0

        if (isDriver) {
            val seatsText = etSeats.text.toString()
            if (seatsText.isEmpty()) {
                etSeats.error = "Please enter available seats"
                return
            }
            seats = seatsText.toIntOrNull() ?: 0
            if (seats <= 0) {
                etSeats.error = "Must have at least 1 seat"
                return
            }
        }

        // Create ride object
        val ride = Ride(
            id = "",
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "Anonymous",
            userPhoto = currentUser.photoUrl?.toString() ?: "",
            destination = destination,
            departureTime = departureTime,
            isDriver = isDriver,
            seats = seats,
            timestamp = System.currentTimeMillis()
        )

        // Save to Firestore
        firestore.collection("rides")
            .add(ride)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Ride created with ID: ${documentReference.id}")
                Toast.makeText(this, "Ride created successfully!", Toast.LENGTH_SHORT).show()

                // Clear inputs
                etDestination.text?.clear()
                etDepartureTime.text?.clear()
                etSeats.text?.clear()
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
//                startActivity(Intent(this, MatchesActivity::class.java))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun onRideSelected(ride: Ride) {
        // Open ride details or request dialog
//        val intent = Intent(this, RideDetailActivity::class.java)
//        intent.putExtra("RIDE_ID", ride.id)
//        startActivity(intent)
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

}