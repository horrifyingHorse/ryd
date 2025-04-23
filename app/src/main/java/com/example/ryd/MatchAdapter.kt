package com.example.ryd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ryd.HomeActivity.RideMatch
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

// Adapter for matches
class MatchAdapter(
    private val matches: List<RideMatch>,
    private val onRideClicked: (RideMatch) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.bind(match)
    }

    override fun getItemCount() = matches.size

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvFromLocation: TextView = itemView.findViewById(R.id.tvFromLocation)
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        private val tvTime: TextView = itemView.findViewById(R.id.tvDepartureTime)
        private val tvMatchType: TextView = itemView.findViewById(R.id.tvMatchType)
        private val tvMatchScore: TextView = itemView.findViewById(R.id.tvMatchScore)
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivUserPhoto)
        private val rideTypeChip: Chip = itemView.findViewById(R.id.chipRideType)

        fun bind(match: RideMatch) {
            val ride = match.ride

            // Set user information
            tvUserName.text = ride.userName

            // Set locations
            tvFromLocation.text = ride.fromLocation
            tvDestination.text = ride.destination

            // Set time
            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            tvTime.text = dateFormat.format(Date(ride.departureTime))

            // Set match info
            tvMatchType.text = match.matchType
            tvMatchScore.text = "${match.score}% match"

            // Set ride type
            rideTypeChip.text = if (ride.isDriver) "Driver" else "Passenger"

            // Set user photo
            if (ride.userPhoto.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(ride.userPhoto)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(ivUserPhoto)
            } else {
                ivUserPhoto.setImageResource(R.drawable.default_profile)
            }

            // Set click listener
            itemView.setOnClickListener { onRideClicked(match) }
        }
    }
}