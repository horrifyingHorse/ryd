package com.example.ryd

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ryd.databinding.ActivityEditRideBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.toString

class EditRideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditRideBinding
    private lateinit var firestore: FirebaseFirestore

    private var rideId: String = ""
    private var departureTime: Long = 0
    private var isDriver: Boolean = false
    private var seats: Int = 0

    // Location coordinates instead of place IDs
    private var fromLatitude: Double = 0.0
    private var fromLongitude: Double = 0.0
    private var toLatitude: Double = 0.0
    private var toLongitude: Double = 0.0

    private var selectedDate: Long = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    companion object {
        private const val FROM_LOCATION_REQUEST_CODE = 1
        private const val TO_LOCATION_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Ride"

        // Get ride data from intent
        rideId = intent.getStringExtra("RIDE_ID") ?: ""
        val fromLocation = intent.getStringExtra("FROM_LOCATION") ?: ""
        val destination = intent.getStringExtra("DESTINATION") ?: ""
        departureTime = intent.getLongExtra("DEPARTURE_TIME", System.currentTimeMillis())
        val description = intent.getStringExtra("DESCRIPTION") ?: ""
        isDriver = intent.getBooleanExtra("IS_DRIVER", false)
        seats = intent.getIntExtra("SEATS", 0)

        // Get coordinates if available
        fromLatitude = intent.getDoubleExtra("FROM_LAT", 0.0)
        fromLongitude = intent.getDoubleExtra("FROM_LNG", 0.0)
        toLatitude = intent.getDoubleExtra("TO_LAT", 0.0)
        toLongitude = intent.getDoubleExtra("TO_LNG", 0.0)

        // Set initial values
        binding.etFrom.setText(fromLocation)
        binding.etTo.setText(destination)
        binding.etDescription.setText(description)
        updateTimeDisplay()

        // Setup listeners
        setupLocationPickers()
        setupTimePicker()

        binding.btnSaveChanges.setOnClickListener {
            saveChanges()
        }
    }

    private fun setupLocationPickers() {
        binding.etFrom.setOnClickListener {
            // Launch map activity for from location
            val intent = Intent(this, MapPickerActivity::class.java)
            intent.putExtra("REQUEST_TYPE", "FROM_LOCATION")

            // Pass current location text if it exists
            val currentLocation = binding.etFrom.text.toString().trim()
            if (currentLocation.isNotEmpty()) {
                intent.putExtra("CURRENT_LOCATION", currentLocation)
            }

            startActivityForResult(intent, FROM_LOCATION_REQUEST_CODE)
        }

        binding.etTo.setOnClickListener {
            // Launch map activity for to location
            val intent = Intent(this, MapPickerActivity::class.java)
            intent.putExtra("REQUEST_TYPE", "DESTINATION_LOCATION")

            // Pass current destination text if it exists
            val currentDestination = binding.etTo.text.toString().trim()
            if (currentDestination.isNotEmpty()) {
                intent.putExtra("CURRENT_LOCATION", currentDestination)
            }
            startActivityForResult(intent, TO_LOCATION_REQUEST_CODE)
        }

        binding.etFrom.isFocusable = false
        binding.etFrom.isClickable = true

        binding.etTo.isFocusable = false
        binding.etTo.isClickable = true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FROM_LOCATION_REQUEST_CODE -> {
                    data?.let {
                        val locationName = it.getStringExtra("LOCATION_NAME") ?: ""
                        binding.etFrom.setText(locationName)
                    }
                }
                TO_LOCATION_REQUEST_CODE -> {
                    data?.let {
                        val locationName = it.getStringExtra("LOCATION_NAME") ?: ""
                        binding.etTo.setText(locationName)
                    }
                }
            }
        }
    }


    private fun setupTimePicker() {
        binding.tvDepartureTime.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun updateTimeDisplay() {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        binding.tvDepartureTime.text = dateFormat.format(Date(departureTime))
    }

    private fun showDateTimePicker() {
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
                binding.tvDepartureTime.text = dateFormat.format(calendar.time)
            }

            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun saveChanges() {
        // Validate inputs
        val fromLocation = binding.etFrom.text.toString().trim()
        val destination = binding.etTo.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (fromLocation.isEmpty()) {
            binding.etFrom.error = "Start location is required"
            return
        }

        if (destination.isEmpty()) {
            binding.etTo.error = "Destination is required"
            return
        }

        // Show progress
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveChanges.isEnabled = false

        // Update ride in Firestore
        val rideUpdates = hashMapOf<String, Any>(
            "fromLocation" to fromLocation,
            "destination" to destination,
            "departureTime" to departureTime,
            "description" to description
        )

        // Add coordinates if available
        if (fromLatitude != 0.0 && fromLongitude != 0.0) {
            rideUpdates["fromLatitude"] = fromLatitude
            rideUpdates["fromLongitude"] = fromLongitude
        }

        if (toLatitude != 0.0 && toLongitude != 0.0) {
            rideUpdates["toLatitude"] = toLatitude
            rideUpdates["toLongitude"] = toLongitude
        }

        firestore.collection("rides").document(rideId)
            .update(rideUpdates)
            .addOnSuccessListener {
                // Update was successful
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Ride updated successfully", Toast.LENGTH_SHORT).show()

                // Notify users about the change
                notifyUsersAboutUpdate(rideUpdates)

                // Close activity and return to previous screen
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnSaveChanges.isEnabled = true
                Toast.makeText(this, "Failed to update ride: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun notifyUsersAboutUpdate(updates: Map<String, Any>) {
        // Get all users who requested or joined this ride
        firestore.collection("rideRequests")
            .whereEqualTo("rideId", rideId)
            .whereIn("status", listOf("accepted", "pending"))
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val requesterId = document.getString("requesterId") ?: continue

                    // Update the ride details in user's rides collection
                    firestore.collection("userRides")
                        .document(requesterId)
                        .collection("rides")
                        .whereEqualTo("originalRideId", rideId)
                        .get()
                        .addOnSuccessListener { userRides ->
                            for (userRide in userRides) {
                                userRide.reference.update(updates)
                            }
                        }
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}