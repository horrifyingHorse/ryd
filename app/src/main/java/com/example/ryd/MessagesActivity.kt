package com.example.ryd

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MessagesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var rvConversations: RecyclerView
    private lateinit var tvNoMessages: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val conversationsList = mutableListOf<Conversation>()
    private lateinit var conversationsAdapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        rvConversations = findViewById(R.id.rvConversations)
        tvNoMessages = findViewById(R.id.tvNoMessages)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messages"

        // Setup RecyclerView
        rvConversations.layoutManager = LinearLayoutManager(this)
        conversationsAdapter = ConversationsAdapter(conversationsList, this::onConversationSelected)
        rvConversations.adapter = conversationsAdapter

        // Load conversations
        loadConversations()
    }

    private fun loadConversations() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("conversations")
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                conversationsList.clear()

                if (documents.isEmpty) {
                    showNoMessages("You don't have any messages yet")
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val conversation = document.toObject(Conversation::class.java).apply {
                        id = document.id
                    }

                    // Fetch other user's details
                    val otherUserId = conversation.participants.find { it != currentUser.uid }
                    if (otherUserId != null) {
                        firestore.collection("users")
                            .document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                conversation.otherUserName = userDoc.getString("name") ?: "User"
                                conversation.otherUserPhoto = userDoc.getString("photoUrl") ?: ""

                                conversationsList.add(conversation)
                                conversationsAdapter.notifyDataSetChanged()

                                tvNoMessages.visibility = View.GONE
                                rvConversations.visibility = View.VISIBLE
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MessagesActivity", "Error loading conversations", e)
                showNoMessages("Error loading messages: ${e.message}")
            }
    }

    private fun showNoMessages(message: String) {
        tvNoMessages.text = message
        tvNoMessages.visibility = View.VISIBLE
        rvConversations.visibility = View.GONE
    }

    private fun onConversationSelected(conversation: Conversation) {
        // Get the other user id from the participants list
        val currentUserId = auth.currentUser?.uid ?: return
        val otherUserId = conversation.participants.find { it != currentUserId } ?: return

        // Navigate to chat screen
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("CONVERSATION_ID", conversation.id)
        intent.putExtra("OTHER_USER_ID", otherUserId)
        intent.putExtra("OTHER_USER_NAME", conversation.otherUserName)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}