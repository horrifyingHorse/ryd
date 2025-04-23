package com.example.ryd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RideRequestAdapter(
    private val requests: List<RideRequest>,
    private val onAccept: ((RideRequest) -> Unit)? = null,
    private val onReject: ((RideRequest) -> Unit)? = null,
    private val showButtons: Boolean = true
) : RecyclerView.Adapter<RideRequestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRiderName: TextView = itemView.findViewById(R.id.tvRiderName)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val buttonContainer: View = itemView.findViewById(R.id.buttonContainer)

        fun bind(request: RideRequest) {
            tvRiderName.text = "${request.riderName} wants to join"

            if (showButtons) {
                buttonContainer.visibility = View.VISIBLE
                btnAccept.setOnClickListener { onAccept?.invoke(request) }
                btnReject.setOnClickListener { onReject?.invoke(request) }
            } else {
                buttonContainer.visibility = View.GONE
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