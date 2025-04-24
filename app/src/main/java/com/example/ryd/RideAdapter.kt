package com.example.ryd

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class RideAdapter(
    private val rides: List<Ride>,
    private val onRideClick: (Ride) -> Unit
) : RecyclerView.Adapter<RideAdapter.RideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rides[position]
        holder.bind(ride)
    }

    override fun getItemCount(): Int = rides.size

    inner class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRiderName: TextView = itemView.findViewById(R.id.tvRiderName)
        private val tvRiderDept: TextView = itemView.findViewById(R.id.tvRiderDept)
        private val tvRideType: TextView = itemView.findViewById(R.id.tvRideType)
        private val tvFrom: TextView = itemView.findViewById(R.id.tvFromLocation)
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        private val tvDepartureTime: TextView = itemView.findViewById(R.id.tvDepartureTime)
        private val ivRiderImage: ImageView = itemView.findViewById(R.id.ivRiderImage)
        private val btnRequestRide: Button = itemView.findViewById(R.id.btnRequestRide)
        private val tvTraveller: TextView = itemView.findViewById(R.id.tvTraveller)

        fun bind(ride: Ride) {
            tvRiderName.text = ride.userName

            // Set rider department and year if available
            if (ride.userDepartment.isNotEmpty() && ride.userYear.isNotEmpty()) {
                tvRiderDept.text = "${ride.userDepartment}, Year ${ride.userYear}"
            } else if (ride.userDepartment.isNotEmpty()) {
                tvRiderDept.text = ride.userDepartment
            } else {
                tvRiderDept.text = "University Student"
            }

            // Set ride type
            tvRideType.text = if (ride.isDriver) "Driver" else "Passenger"
            tvTraveller.text = if (ride.isDriver) "Driver" else "Passenger"

            // Set destination
            tvFrom.text = ride.fromLocation
            tvDestination.text = ride.destination

            tvFrom.setOnClickListener {
                openLocationInMap(ride.fromLocation, itemView.context)
            }

            // Make destination clickable to open in maps
            tvDestination.setOnClickListener {
                openLocationInMap(ride.destination, itemView.context)
            }

            // Format departure time
            val dateFormat = SimpleDateFormat("E, MMM d, h:mm a", Locale.getDefault())
            val departureDate = Date(ride.departureTime)
            tvDepartureTime.text = dateFormat.format(departureDate)

            // Set seats info
//            if (ride.isDriver) {
//                tvSeatsAvailable.text = "${ride.seats} seats available"
//                tvSeatsAvailable.visibility = View.VISIBLE
//            } else {
//                tvSeatsAvailable.visibility = View.GONE
//            }

            // Load user photo if available
            if (ride.userPhoto.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(ride.userPhoto)
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(ivRiderImage)
            } else {
                ivRiderImage.setImageResource(R.drawable.default_profile)
            }

            // Set up request button
            btnRequestRide.setOnClickListener {
                onRideClick(ride)
            }

            // Make the entire item clickable
            itemView.setOnClickListener {
                onRideClick(ride)
            }
        }

        private fun openLocationInMap(location: String, context: Context) {
            try {
                // Create an intent to your custom map activity
                val mapIntent = Intent(context, MapPickerActivity::class.java).apply {
                    // Pass location as an extra
                    putExtra("LOCATION_NAME", location)
                    // Add a flag to indicate this is for viewing only
                    putExtra("VIEW_ONLY_MODE", true)
                }
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open map", Toast.LENGTH_SHORT).show()
                Log.e("RideAdapter", "Error opening map: ${e.message}")
            }
        }
    }
}