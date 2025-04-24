package com.example.ryd

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MyRidesAdapter(
    private val rides: List<Ride>,
    private val onRideClick: (Ride) -> Unit,
) : RecyclerView.Adapter<MyRidesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRideType: TextView = itemView.findViewById(R.id.tvRideType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvFromLocation: TextView = itemView.findViewById(R.id.tvFromLocation)
        val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        val tvDepartureTime: TextView = itemView.findViewById(R.id.tvDepartureTime)
        val tvSeatsLabel: TextView = itemView.findViewById(R.id.tvSeatsLabel)
        val tvSeats: TextView = itemView.findViewById(R.id.tvSeats)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_ride, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ride = rides[position]
        val context = holder.itemView.context

        // Set ride type
        holder.tvRideType.text = if (ride.isDriver) "Driver" else "Passenger"
        holder.tvRideType.setBackgroundResource(
            if (ride.isDriver) R.drawable.bg_ride_status else R.drawable.bg_badge
        )

        if (ride.status == "accepted") {
            // For accepted rides, show Cancel Trip instead of Request Accepted
            holder.btnCancel.text = "Cancel Request"
            holder.btnEdit.visibility = View.GONE

            holder.btnCancel.setOnClickListener {
                // Show confirmation dialog
                AlertDialog.Builder(context)
                    .setTitle("Cancel Trip")
                    .setMessage("Are you sure you want to cancel this trip? This cannot be undone.")
                    .setPositiveButton("Cancel Trip") { _, _ ->
                        cancelRideRequest(context, ride)
                    }
                    .setNegativeButton("Keep Trip", null)
                    .show()
            }
        } else {
            // Set ride status based on status field
            when (ride.status) {
                "requested" -> {
                    holder.tvStatus.text = "Requested"
                    holder.tvStatus.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.colorRequested)
                    )
                    // Hide edit button for requested rides
                    holder.btnEdit.visibility = View.GONE
                    holder.btnCancel.text = "Cancel Request"
                }

                "posted" -> {
                    holder.tvStatus.text = "Upcoming"
                    holder.tvStatus.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )
                    holder.btnEdit.visibility = View.VISIBLE
                    holder.btnCancel.text = "Cancel"
                }

                else -> {
                    holder.tvStatus.text = "Upcoming"
                    holder.tvStatus.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.colorAccent)
                    )
                }
            }
        }
        // Set other ride details
        holder.tvFromLocation.text = ride.fromLocation
        holder.tvDestination.text = ride.destination
        holder.tvFromLocation.setOnClickListener {
            openLocationInMap(ride.fromLocation, context)
        }

        holder.tvDestination.setOnClickListener {
            openLocationInMap(ride.destination, context)
        }

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val departureDate = Date(ride.departureTime)
        holder.tvDepartureTime.text = dateFormat.format(departureDate)

        // Only show seats for driver rides
        if (ride.isDriver) {
            holder.tvSeatsLabel.visibility = View.VISIBLE
            holder.tvSeats.visibility = View.VISIBLE
            holder.tvSeats.text = "${ride.seats} available"
        } else {
            holder.tvSeatsLabel.visibility = View.GONE
            holder.tvSeats.visibility = View.GONE
        }

        // Set click listeners
        holder.itemView.setOnClickListener { onRideClick(ride) }
        holder.btnEdit.setOnClickListener {
            editRide(holder.itemView.context, ride)
        }
        holder.btnCancel.setOnClickListener {
            // Handle cancel differently based on status
            if (ride.status == "requested" || ride.status == "accepted") {
                // Cancel request
                cancelRideRequest(context, ride)
            } else {
                // Cancel ride
                cancelRide(context, ride)
            }
        }
    }

    private fun openLocationInMap(location: String, context: Context) {
        try {
            // Create an intent to your MapPickerActivity
            val mapIntent = Intent(context, MapPickerActivity::class.java).apply {
                // Pass location as an extra
                putExtra("CURRENT_LOCATION", location)
                // Add a flag to indicate this is for viewing only
                putExtra("VIEW_ONLY_MODE", true)
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
            Log.e("MyRidesAdapter", "Error opening map: ${e.message}")
        }
    }

    private fun cancelRide(ride: Ride, holder: ViewHolder) {
        // Show loading
        holder.btnCancel.isEnabled = false

        val context = holder.itemView.context
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Update the ride request status to "cancelled"
        firestore.collection("rideRequests")
            .whereEqualTo("rideId", ride.id)
            .whereEqualTo("riderId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showError(holder, "Request not found")
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()

                // Update request status
                for (document in documents) {
                    batch.update(document.reference, "status", "cancelled")
                }

                // Update user ride copy
                firestore.collection("userRides")
                    .document(userId)
                    .collection("rides")
                    .document(ride.id)
                    .update("status", "cancelled")
                    .addOnSuccessListener {
                        // Update original ride to free up a seat if this was a passenger
//                        if (!ride.isDriver) {
//                            firestore.collection("rides")
//                                .document(ride.originalRideId ?: ride.id)
//                                .get()
//                                .addOnSuccessListener { rideDoc ->
//                                    val currentSeats = rideDoc.getLong("availableSeats") ?: 0
//                                    rideDoc.reference.update(
//                                        "availableSeats", currentSeats + 1
//                                    )
//                                }
//                        }

                        // Commit all changes
                        batch.commit()
                            .addOnSuccessListener {
                                // Success, update UI
                                holder.btnCancel.text = "Cancelled"
                                holder.btnCancel.isEnabled = false

                                // Use a system color since colorGrey is not defined
                                holder.btnCancel.setBackgroundColor(
                                    ContextCompat.getColor(context, android.R.color.darker_gray)
                                )

                                // Notify ride creator about cancellation
                                if (!ride.isDriver) {
                                    sendCancellationNotification(ride)
                                }

                                Toast.makeText(context, "Trip cancelled successfully",
                                    Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                showError(holder, "Failed to cancel: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        showError(holder, "Failed to cancel: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showError(holder, "Failed to cancel: ${e.message}")
            }
    }

    private fun showError(holder: ViewHolder, message: String) {
//        holder.progressBar.visibility = View.GONE
        val context = holder.itemView.context
        holder.btnCancel.isEnabled = true
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Optional: Send notification to the other party
    private fun sendCancellationNotification(ride: Ride) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "A user"

        // Determine the recipient (if current user is driver, notify passenger, and vice versa)
        val recipientId = ride.userId
//            if (ride.isDriver) {
//            ride.userId // Notify passenger
//        } else {
//            ride.driverId // Notify driver
//        }

        // Skip if no recipient
        if (recipientId.isNullOrEmpty()) return

        // Create notification document
        val notification = hashMapOf(
            "userId" to recipientId,
            "title" to "Trip Cancelled",
            "message" to "$currentUserName has cancelled the trip from ${ride.fromLocation} to ${ride.destination}",
            "timestamp" to System.currentTimeMillis(),
            "read" to false,
            "rideId" to ride.id
        )

        // Save notification
        firestore.collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                // Notification created successfully
            }
            .addOnFailureListener {
                // Failed to create notification, but we don't need to bother the user about this
            }
    }

    private fun cancelRideRequest(context: Context, ride: Ride) {
        // Code to cancel a ride request
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return

        // Find the request ID
        firestore.collection("rideRequests")
            .whereEqualTo("rideId", ride.id)
            .whereEqualTo("riderId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val requestId = documents.documents[0].id

                    // Delete the request
                    firestore.collection("rideRequests")
                        .document(requestId)
                        .delete()
                        .addOnSuccessListener {
                            // Also delete from userRides
                            firestore.collection("userRides")
                                .document(currentUser.uid)
                                .collection("rides")
                                .whereEqualTo("requestId", requestId)
                                .get()
                                .addOnSuccessListener { userRides ->
                                    for (doc in userRides) {
                                        doc.reference.delete()
                                    }
                                    Toast.makeText(context, "Request canceled", Toast.LENGTH_SHORT).show()
                                    notifyDataSetChanged()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to cancel request", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun cancelRide(context: Context, ride: Ride) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return

        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Cancel Ride")
            .setMessage("Are you sure you want to cancel this ride?")
            .setPositiveButton("Yes") { _, _ ->
                // Delete the ride document
                firestore.collection("rides").document(ride.id)
                    .delete()
                    .addOnSuccessListener {
                        // Now notify any users who joined/requested this ride
                        notifyUsersAboutCancellation(context, ride, firestore)
                        Toast.makeText(context, "Ride canceled successfully", Toast.LENGTH_SHORT).show()

                        // Remove from the activity's list will happen on activity resume/refresh
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Failed to cancel ride: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun notifyUsersAboutCancellation(context: Context, ride: Ride, firestore: FirebaseFirestore) {
        // Get all users who requested or joined this ride
        firestore.collection("rideRequests")
            .whereEqualTo("rideId", ride.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val requesterId = document.getString("requesterId") ?: continue

                    // Update the status in the user's ride collection
                    firestore.collection("userRides")
                        .document(requesterId)
                        .collection("rides")
                        .whereEqualTo("originalRideId", ride.id)
                        .get()
                        .addOnSuccessListener { userRidesDocs ->
                            for (userRideDoc in userRidesDocs) {
                                userRideDoc.reference.update("status", "cancelled by host")
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to notify users about cancellation", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editRide(context: Context, ride: Ride) {
        val intent = Intent(context, EditRideActivity::class.java).apply {
            putExtra("RIDE_ID", ride.id)
            putExtra("FROM_LOCATION", ride.fromLocation)
            putExtra("DESTINATION", ride.destination)
            putExtra("DEPARTURE_TIME", ride.departureTime)
            putExtra("DESCRIPTION", ride.description)
            putExtra("IS_DRIVER", ride.isDriver)
            putExtra("SEATS", ride.seats)
        }
        context.startActivity(intent)
    }

    override fun getItemCount() = rides.size
}