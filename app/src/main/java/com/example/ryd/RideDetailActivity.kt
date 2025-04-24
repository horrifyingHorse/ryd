package com.example.ryd

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.get
import kotlin.text.clear
import kotlin.text.format
import kotlin.text.get

class RideDetailActivity : AppCompatActivity() {
    private var userRequestId: String? = null // Store the user's request ID

    private lateinit var toolbar: Toolbar
    private lateinit var tvRiderName: TextView
    private lateinit var tvRiderSubtitle: TextView
    private lateinit var tvRideType: TextView
    private lateinit var tvFromLocation: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvDepartureTime: TextView
    private lateinit var tvSeatsAvailable: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnRequestRide: Button
    private lateinit var btnMessageRider: Button

    private lateinit var requestsCard: View
    private lateinit var confirmedCard: View
    private lateinit var rvRequests: RecyclerView
    private lateinit var rvConfirmed: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var tvNoConfirmed: TextView

    private lateinit var requestsAdapter: RideRequestAdapter
    private lateinit var confirmedAdapter: RideRequestAdapter

    private val rideRequests = mutableListOf<RideRequest>()
    private val confirmedRiders = mutableListOf<RideRequest>()

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

        rideId = intent.getStringExtra("rideId") ?: ""
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
        tvRiderSubtitle = findViewById(R.id.tvRiderSubtitle)
        tvRideType = findViewById(R.id.tvRideType)
        tvFromLocation = findViewById(R.id.tvFromLocation)
        tvDestination = findViewById(R.id.tvDestination)
        tvDepartureTime = findViewById(R.id.tvDepartureTime)
        tvSeatsAvailable = findViewById(R.id.tvSeatsAvailable)
        tvDescription = findViewById(R.id.tvDescription)
        btnRequestRide = findViewById(R.id.btnRequestRide)
        btnMessageRider = findViewById(R.id.btnMessageRider)

        requestsCard = findViewById(R.id.requestsCard)
        confirmedCard = findViewById(R.id.confirmedCard)
        rvRequests = findViewById(R.id.rvRequests)
        rvConfirmed = findViewById(R.id.rvConfirmed)
        tvNoRequests = findViewById(R.id.tvNoRequests)
        tvNoConfirmed = findViewById(R.id.tvNoConfirmed)

        requestsCard.visibility = View.VISIBLE
        confirmedCard.visibility = View.VISIBLE

        setupRecyclerViews()

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

    private fun setupRecyclerViews() {
        // Setup request adapter
        requestsAdapter = RideRequestAdapter(
            rideRequests,
            onAccept = { request -> acceptRequest(request) },
            onReject = { request -> rejectRequest(request) }
        )
        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = requestsAdapter

        // Setup confirmed adapter
        confirmedAdapter = RideRequestAdapter(requests=confirmedRiders, showButtons = false)
        rvConfirmed.layoutManager = LinearLayoutManager(this)
        rvConfirmed.adapter = confirmedAdapter
    }


    private fun loadRideDetails() {
        val currentUserId = auth.currentUser?.uid

        firestore.collection("rides")
            .document(rideId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ride = document.toObject(Ride::class.java)?.apply { id = document.id }
                    updateUI()

                    // Check if the current user has already sent a request
                    checkExistingRequest()

                    // Only load requests if current user is the ride owner
                    if (ride?.userId == currentUserId) {
                        loadRideRequests()
//                    } else {
//                        requestsCard.visibility = View.GONE
//                        confirmedCard.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this, "Ride not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

    }

    private fun checkExistingRequest() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("rideRequests")
            .whereEqualTo("rideId", rideId)
            .whereEqualTo("riderId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // User has already sent a request
                    val request = documents.documents[0]
                    userRequestId = request.id

                    when (request.getString("status")) {
                        "pending" -> {
                            btnRequestRide.text = "Cancel Request"
                            btnRequestRide.isEnabled = true
                        }
                        "accepted" -> {
                            btnRequestRide.text = "Cancel Request"
                            btnRequestRide.isEnabled = true
                        }
                        "rejected" -> {
                            btnRequestRide.text = "Request Rejected"
                            btnRequestRide.isEnabled = false
                        }
                    }
                } else {
                    // No existing request
                    btnRequestRide.text = "Join Trip"
                    btnRequestRide.isEnabled = true
                    userRequestId = null
                }
            }
            .addOnFailureListener { e ->
                Log.e("RideDetailActivity", "Error checking request: ${e.message}")
            }
    }

    private fun updateUI() {
        ride?.let { ride ->
            tvRiderName.text = ride.userName
            tvRideType.text = if (ride.isDriver) "Driver" else "Passenger"
            tvFromLocation.text = ride.fromLocation
            tvDestination.text = ride.destination

            loadRideCreatorInfo()

            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val departureDate = Date(ride.departureTime)
            tvDepartureTime.text = dateFormat.format(departureDate)

            if (ride.description.isNotEmpty()) {
                tvDescription.text = ride.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            // Show seats available only for drivers
            if (ride.isDriver) {
                tvSeatsAvailable.visibility = View.VISIBLE
                tvSeatsAvailable.text = "${ride.seats} seats available"
            } else {
                tvSeatsAvailable.visibility = View.GONE
            }

            val currentUserId = auth.currentUser?.uid

            // Always show request and confirmed cards, but only show accept/deny buttons to ride owner
            requestsCard.visibility = View.VISIBLE
            confirmedCard.visibility = View.VISIBLE

            // Show Join Trip button only if not ride owner
            if (ride.userId == currentUserId) {
                // Current user is the ride owner
                btnRequestRide.visibility = View.GONE
                btnMessageRider.visibility = View.GONE
            } else {
                // Current user is not the ride owner
                btnRequestRide.visibility = View.VISIBLE
                btnMessageRider.visibility = View.VISIBLE
                checkExistingRequest()
            }

            // Always load ride requests regardless of who's viewing
            loadRideRequests()
        }
    }
    private fun loadRideRequests() {
        firestore.collection("rideRequests")
            .whereEqualTo("rideId", rideId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("RideDetailActivity", "Found ${documents.size()} ride requests")

                rideRequests.clear()
                confirmedRiders.clear()

                for (document in documents) {
                    val request = document.toObject(RideRequest::class.java).apply {
                        id = document.id
                    }
                    Log.d("RideDetailActivity", "Request: ${request.riderName}, status: ${request.status}")

                    when (request.status) {
                        "pending" -> rideRequests.add(request)
                        "accepted" -> confirmedRiders.add(request)
                    }
                }

                // Update UI with results
                updateRequestsUI()
                updateConfirmedUI()
            }
            .addOnFailureListener { e ->
                Log.e("RideDetailActivity", "Error loading requests: ${e.message}", e)
                Toast.makeText(this, "Failed to load ride requests", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateRequestsUI() {
        if (rideRequests.isEmpty()) {
            tvNoRequests.visibility = View.VISIBLE
            rvRequests.visibility = View.GONE
        } else {
            tvNoRequests.visibility = View.GONE
            rvRequests.visibility = View.VISIBLE

            // Initialize adapter if not already done
            if (rvRequests.adapter == null) {
                rvRequests.layoutManager = LinearLayoutManager(this)
                rvRequests.adapter = RideRequestAdapter(
                    requests = rideRequests,
                    onAccept = { request -> acceptRequest(request) },
                    onReject = { request -> rejectRequest(request) }
                )
            } else {
                (rvRequests.adapter as RideRequestAdapter).notifyDataSetChanged()
            }
        }
    }
    private fun updateConfirmedUI() {
        if (confirmedRiders.isEmpty()) {
            tvNoConfirmed.visibility = View.VISIBLE
            rvConfirmed.visibility = View.GONE
        } else {
            tvNoConfirmed.visibility = View.GONE
            rvConfirmed.visibility = View.VISIBLE

            // Initialize adapter if not already done
            if (rvConfirmed.adapter == null) {
                rvConfirmed.layoutManager = LinearLayoutManager(this)
                rvConfirmed.adapter = RideRequestAdapter(
                    requests = confirmedRiders,
                    showButtons = false
                )
            } else {
                (rvConfirmed.adapter as RideRequestAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun acceptRequest(request: RideRequest) {
        firestore.collection("rideRequests")
            .document(request.id)
            .update("status", "accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Request accepted", Toast.LENGTH_SHORT).show()

                // Update the user's ride status
                updateUserRideStatus(request, "accepted")

                // Reload requests
                loadRideRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(request: RideRequest) {
        firestore.collection("rideRequests")
            .document(request.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()

                // Update the user's ride status
                updateUserRideStatus(request, "rejected")

                // Reload requests
                loadRideRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to reject request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserRideStatus(request: RideRequest, status: String) {
        firestore.collection("userRides")
            .document(request.riderId)
            .collection("rides")
            .whereEqualTo("requestId", request.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("status", status)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RideDetailActivity", "Error updating user ride status: ${e.message}", e)
            }
    }

    private fun requestRide() {
        val currentUser = auth.currentUser ?: return
        val currentRide = ride ?: return

        if (userRequestId != null) {
            cancelRequest()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Join Trip")
            .setMessage("Would you like to join this trip from ${currentRide.fromLocation} to ${currentRide.destination}?")
            .setPositiveButton("Yes") { _, _ ->
                // Create a ride request
                val request = hashMapOf(
                    "rideId" to rideId,
                    "riderId" to currentUser.uid,
                    "riderName" to (currentUser.displayName ?: "Anonymous"),
                    "driverId" to currentRide.userId,
                    "status" to "pending",
                    "timestamp" to System.currentTimeMillis(),
                    "fromLocation" to currentRide.fromLocation,
                    "destination" to currentRide.destination
                )

                firestore.collection("rideRequests")
                    .add(request)
                    .addOnSuccessListener { documentRef ->
                        userRequestId = documentRef.id
                        btnRequestRide.text = "Cancel Request"
                        addRideToMyRides(currentRide, documentRef.id)
                        Toast.makeText(this, "Trip join request sent!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addRideToMyRides(ride: Ride, requestId: String) {
        val currentUser = auth.currentUser ?: return

        // Create a user ride entry with the requested status
        val userRide = hashMapOf(
            "originalRideId" to ride.id,
            "requestId" to requestId,
            "userId" to currentUser.uid,
            "ridePosterName" to ride.userName,
            "ridePosterUserId" to ride.userId,
            "fromLocation" to ride.fromLocation,
            "destination" to ride.destination,
            "departureTime" to ride.departureTime,
            "status" to "requested",
            "isDriver" to ride.isDriver,
            "seats" to ride.seats,
            "description" to ride.description,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("userRides")
            .document(currentUser.uid)
            .collection("rides")
            .add(userRide)
            .addOnSuccessListener {
                Log.d("RideDetailActivity", "Ride added to My Rides")
            }
            .addOnFailureListener { e ->
                Log.e("RideDetailActivity", "Error adding ride to My Rides", e)
            }
    }

    private fun cancelRequest() {
        val requestId = userRequestId ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Request")
            .setMessage("Are you sure you want to cancel your join request?")
            .setPositiveButton("Yes") { _, _ ->
                firestore.collection("rideRequests")
                    .document(requestId)
                    .delete()
                    .addOnSuccessListener {
                        userRequestId = null
                        btnRequestRide.text = "Join Trip"
                        removeRideFromMyRides(requestId)
                        Toast.makeText(this, "Request canceled", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to cancel request: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeRideFromMyRides(requestId: String) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("userRides")
            .document(currentUser.uid)
            .collection("rides")
            .whereEqualTo("requestId", requestId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RideDetailActivity", "Error removing ride from My Rides", e)
            }
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
                     val intent = Intent(this, ChatActivity::class.java)
                     intent.putExtra("CONVERSATION_ID", existingConversationId)
//                     intent.putExtra("OTHER_USER_ID", otherUserId)
//                     intent.putExtra("OTHER_USER_NAME", otherUserName)
                     startActivity(intent)
                    Toast.makeText(this, "This would open the chat screen", Toast.LENGTH_SHORT).show()
                } else {
                    // Create new conversation
                    val conversation = hashMapOf(
                        "participants" to listOf(currentUser.uid, currentRide.userId),
                        "lastMessage" to "Hello, I'm interested in your ride",
                        "lastMessageTimestamp" to System.currentTimeMillis(),
                        "ride" to hashMapOf(
                            "id" to rideId,
                            "fromLocation" to currentRide.fromLocation,
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
                                    val intent = Intent(this, ChatActivity::class.java)
                                    intent.putExtra("CONVERSATION_ID", documentRef.id)
                                    startActivity(intent)
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

    private fun loadRideCreatorInfo() {
        ride?.userId?.let { userId ->
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
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

                        // Update the subtitle text
                        tvRiderSubtitle.text = "$yearText, $branch"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting user info", exception)
                }
        }
    }
}