package com.example.ryd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ConversationsAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserPhoto: ImageView = itemView.findViewById(R.id.ivUserPhoto)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvDestination: TextView = itemView.findViewById(R.id.tvDestination)

        fun bind(conversation: Conversation) {
            tvUserName.text = conversation.otherUserName
            tvLastMessage.text = conversation.lastMessage

            // Format timestamp
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvTimestamp.text = dateFormat.format(Date(conversation.lastMessageTimestamp))

            // Show ride destination if available
            val destination = conversation.ride["destination"]
            if (!destination.isNullOrEmpty()) {
                tvDestination.visibility = View.VISIBLE
                tvDestination.text = "Ride to $destination"
            } else {
                tvDestination.visibility = View.GONE
            }

            // Load user photo
            if (conversation.otherUserPhoto.isNotEmpty()) {
                Picasso.get()
                    .load(conversation.otherUserPhoto)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(ivUserPhoto)
            } else {
                ivUserPhoto.setImageResource(R.drawable.default_profile)
            }

            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }
    }
}