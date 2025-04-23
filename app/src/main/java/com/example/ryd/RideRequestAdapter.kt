package com.example.ryd

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RideRequestAdapter(
    private val requests: List<RideRequest>,
    private val onAccept: ((RideRequest) -> Unit)? = null,
    private val onReject: ((RideRequest) -> Unit)? = null,
    private val showButtons: Boolean = true
) : RecyclerView.Adapter<RideRequestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        private val tvRiderName: TextView = itemView.findViewById(R.id.tvRiderName)
        private val tvUserInfo: TextView = itemView.findViewById(R.id.tvUserInfo)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val buttonContainer: View = itemView.findViewById(R.id.buttonContainer)
        private var isInfoExpanded = false

        fun bind(request: RideRequest) {
            // Set rider name
            tvRiderName.text = request.riderName

            // Set profile image (using default for now)
            ivProfilePic.setImageResource(R.drawable.default_profile)

            // Set initial visibility
            tvUserInfo.visibility = View.GONE
            tvDescription.visibility = View.GONE

            // Show/hide buttons based on if current user is the ride owner
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isRideOwner = request.driverId == currentUserId

            if (showButtons && isRideOwner) {
                buttonContainer.visibility = View.VISIBLE
                btnAccept.setOnClickListener { onAccept?.invoke(request) }
                btnReject.setOnClickListener { onReject?.invoke(request) }
            } else {
                buttonContainer.visibility = View.GONE
            }

            // Load user information from Firestore
            loadUserInfo(request.riderId, tvUserInfo, tvDescription)

            // Set click listener for the entire item to toggle info visibility
            itemView.setOnClickListener {
                isInfoExpanded = !isInfoExpanded
                tvUserInfo.visibility = if (isInfoExpanded) View.VISIBLE else View.GONE
                tvDescription.visibility = if (isInfoExpanded && tvDescription.text.isNotEmpty())
                    View.VISIBLE else View.GONE
            }
        }

        private fun loadUserInfo(userId: String, tvUserInfo: TextView, tvDescription: TextView) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Extract academic year and branch
                        val academicYear = document.getLong("academicYear")?.toInt() ?: 0
                        val branch = document.getString("branch") ?: "Unknown"
                        val description = document.getString("description") ?: ""

                        // Format the year text
                        val yearText = when (academicYear) {
                            1 -> "1st year"
                            2 -> "2nd year"
                            3 -> "3rd year"
                            4 -> "4th year"
                            else -> "$academicYear year"
                        }

                        // Set the formatted text
                        tvUserInfo.text = "$yearText, $branch"

                        // Set description if available
                        if (description.isNotEmpty()) {
                            tvDescription.text = description
                            tvDescription.visibility = if (isInfoExpanded) View.VISIBLE else View.GONE
                        } else {
                            tvDescription.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("RideRequestAdapter", "Error getting user info", exception)
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size
}