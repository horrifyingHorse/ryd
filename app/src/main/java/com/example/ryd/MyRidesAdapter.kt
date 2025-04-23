package com.example.ryd

import android.content.Context
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
    private val onRideClick: (Ride) -> Unit
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

        // Set other ride details
        holder.tvFromLocation.text = ride.fromLocation
        holder.tvDestination.text = ride.destination

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
        holder.btnEdit.setOnClickListener { onRideClick(ride) }
        holder.btnCancel.setOnClickListener {
            // Handle cancel differently based on status
            if (ride.status == "requested") {
                // Cancel request
                cancelRideRequest(context, ride)
            } else {
                // Cancel ride
                cancelRide(context, ride)
            }
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
        // Existing code to cancel a posted ride
    }

    override fun getItemCount() = rides.size
}