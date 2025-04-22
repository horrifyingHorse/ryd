package com.example.ryd

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RideDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvRiderName: TextView
    private lateinit var tvRideType: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvDepartureTime: TextView
    private lateinit var tvSeatsAvailable: TextView
    private lateinit var btnRequestRide: Button
    private lateinit var btnMessageRider: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var rideId: String
    private var ride: Ride? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ride_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rideId = intent.getStringExtra("RIDE_ID") ?: ""
        if (rideId.isEmpty()) {
            Toast.makeText(this, "Error: Ride information missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        tvRiderName = findViewById(R.id.tvRiderName)
        tvRideType = findViewById(R.id.tvRideType)
        tvDestination = findViewById(R.id.tvDestination)
        tvDepartureTime = findViewById(R.id.tvDepartureTime)
        tvSeatsAvailable = findViewById(R.id.tvSeatsAvailable)
        btnRequestRide = findViewById(R.id.btnRequestRide)
        btnMessageRider = findViewById(R.id.btnMessageRider)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ride Details"

        // Load ride details
        loadRideDetails()

        // Setup click listeners
        btnRequestRide.setOnClickListener {
            requestRide()
        }

        btnMessageRider.setOnClickListener {
            messageRider()
        }
    }

    private fun loadRideDetails() {
        firestore.collection("rides")
            .document(rideId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ride = document.toObject(Ride::class.java)?.apply { id = document.id }
                    updateUI()
                } else {
                    Toast.makeText(this, "Ride not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading ride: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateUI() {
        ride?.let { ride ->
            tvRiderName.text = ride.userName
            tvRideType.text = if (ride.isDriver) "Driver" else "Passenger"
            tvDestination.text = ride.destination

            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val departureDate = Date(ride.departureTime)
            tvDepartureTime.text = dateFormat.format(departureDate)

            if (ride.isDriver) {
                tvSeatsAvailable.visibility = View.VISIBLE
                tvSeatsAvailable.text = "${ride.seats} seats available"
                btnRequestRide.visibility = View.VISIBLE
            } else {
                tvSeatsAvailable.visibility = View.GONE
                btnRequestRide.visibility = View.GONE
            }
        }
    }

    private fun requestRide() {
        val currentUser = auth.currentUser ?: return
        val currentRide = ride ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Request Ride")
            .setMessage("Would you like to request a ride to ${currentRide.destination}?")
            .setPositiveButton("Yes") { _, _ ->
                // Create a ride request
                val request = hashMapOf(
                    "rideId" to rideId,
                    "riderId" to currentUser.uid,
                    "riderName" to (currentUser.displayName ?: "Anonymous"),
                    "driverId" to currentRide.userId,
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis(),
                    "destination" to currentRide.destination
                )

                firestore.collection("rideRequests")
                    .add(request)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ride request sent!", Toast.LENGTH_SHORT).show()
                        btnRequestRide.isEnabled = false
                        btnRequestRide.text = "Request Sent"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun messageRider() {
        val currentUser = auth.currentUser ?: return
        val currentRide = ride ?: return

        // Check if conversation already exists
        firestore.collection("conversations")
            .whereArrayContains("participants", currentUser.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var existingConversationId: String? = null

                for (document in querySnapshot.documents) {
                    val participants = document.get("participants") as? List<String>
                    if (participants != null && participants.contains(currentRide.userId)) {
                        existingConversationId = document.id
                        break
                    }
                }

                if (existingConversationId != null) {
                    // Open existing conversation
                    // val intent = Intent(this, ChatActivity::class.java)
                    // intent.putExtra("CONVERSATION_ID", existingConversationId)
                    // startActivity(intent)
                    Toast.makeText(this, "This would open the chat screen", Toast.LENGTH_SHORT).show()
                } else {
                    // Create new conversation
                    val conversation = hashMapOf(
                        "participants" to listOf(currentUser.uid, currentRide.userId),
                        "lastMessage" to "Hello, I'm interested in your ride",
                        "lastMessageTimestamp" to System.currentTimeMillis(),
                        "ride" to hashMapOf(
                            "id" to rideId,
                            "destination" to currentRide.destination
                        )
                    )

                    firestore.collection("conversations")
                        .add(conversation)
                        .addOnSuccessListener { documentRef ->
                            // Add initial message
                            val message = hashMapOf(
                                "conversationId" to documentRef.id,
                                "senderId" to currentUser.uid,
                                "text" to "Hello, I'm interested in your ride",
                                "timestamp" to System.currentTimeMillis()
                            )

                            firestore.collection("messages")
                                .add(message)
                                .addOnSuccessListener {
                                    // Open new conversation
                                    // val intent = Intent(this, ChatActivity::class.java)
                                    // intent.putExtra("CONVERSATION_ID", documentRef.id)
                                    // startActivity(intent)
                                    Toast.makeText(this, "This would open the new chat screen", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to start conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}