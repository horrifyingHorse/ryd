package com.example.ryd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MatchAdapter(
    private val matches: List<Ride>,
    private val onMatchClick: (Ride) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount(): Int = matches.size

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserType: TextView = itemView.findViewById(R.id.tvUserType)
        private val tvFrom: TextView = itemView.findViewById(R.id.tvFromLocation)
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)
        private val tvDepartureTime: TextView = itemView.findViewById(R.id.tvDepartureTime)
        private val tvMatchPercent: TextView = itemView.findViewById(R.id.tvMatchPercent)
        private val btnContact: Button = itemView.findViewById(R.id.btnContact)

        fun bind(match: Ride) {
            tvUserName.text = match.userName
            tvUserType.text = if (match.isDriver) "Driver" else "Passenger"
            tvFrom.text = match.fromLocation
            tvDestination.text = match.destination

            val dateFormat = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
            tvDepartureTime.text = dateFormat.format(Date(match.departureTime))

            // For demonstration purposes - match percentage is random
            val matchPercent = (70..100).random()
            tvMatchPercent.text = "$matchPercent% Match"

            btnContact.setOnClickListener {
                onMatchClick(match)
            }

            itemView.setOnClickListener {
                onMatchClick(match)
            }
        }
    }
}