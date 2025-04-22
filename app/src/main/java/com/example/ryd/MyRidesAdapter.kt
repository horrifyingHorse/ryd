package com.example.ryd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MyRidesAdapter(
    private val rides: List<Ride>,
    private val onRideClick: (Ride) -> Unit
) : RecyclerView.Adapter<MyRidesAdapter.RideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(rides[position])
    }

    override fun getItemCount(): Int = rides.size

    inner class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRideType: TextView = itemView.findViewById(R.id.tvRideType)
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        private val tvDepartureTime: TextView = itemView.findViewById(R.id.tvDepartureTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(ride: Ride) {
            tvRideType.text = if (ride.isDriver) "Driver" else "Passenger"
            tvDestination.text = ride.destination

            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            tvDepartureTime.text = dateFormat.format(Date(ride.departureTime))

            // Determine status based on departure time
            val now = System.currentTimeMillis()
            val status = when {
                ride.departureTime > now -> "Upcoming"
                else -> "Completed"
            }
            tvStatus.text = status

            itemView.setOnClickListener {
                onRideClick(ride)
            }
        }
    }
}