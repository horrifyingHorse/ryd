package com.example.ryd

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.view.View
import com.google.firebase.firestore.Query
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvChatHeader: TextView
    private lateinit var tvNoMessages: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var conversationId: String
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String

    private val messagesList = mutableListOf<Message>()
    private lateinit var messagesAdapter: MessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get conversation details from intent
        conversationId = intent.getStringExtra("CONVERSATION_ID") ?: ""
        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "Chat"

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvChatHeader = findViewById(R.id.tvChatHeader)
        tvNoMessages = findViewById(R.id.tvNoMessages)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = otherUserName

        // Setup RecyclerView
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages start from bottom
        }
        messagesAdapter = MessagesAdapter(messagesList, auth.currentUser?.uid ?: "")
        rvMessages.adapter = messagesAdapter

        // Setup send button
        btnSend.setOnClickListener {
            sendMessage()
        }

        // If we have a conversation ID, load existing messages
        if (conversationId.isNotEmpty()) {
            loadMessages()
        } else if (otherUserId.isNotEmpty()) {
            // Creating a new conversation
            tvNoMessages.text = "Send a message to start a conversation"
            tvNoMessages.visibility = View.VISIBLE
        } else {
            Log.e("ChatActivity", "No conversation ID or other user ID provided")
            finish()
        }
    }

    private fun loadMessages() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                messagesList.clear()

                if (documents.isEmpty) {
                    tvNoMessages.visibility = View.VISIBLE
                    tvNoMessages.text = "No messages yet"
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val message = document.toObject(Message::class.java).apply {
                        id = document.id
                    }
                    messagesList.add(message)
                }

                // Update UI
                tvNoMessages.visibility = View.GONE
                messagesAdapter.notifyDataSetChanged()
                rvMessages.scrollToPosition(messagesList.size - 1)

                markMessagesAsRead()
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error loading messages", e)
                tvNoMessages.text = "Error loading messages"
                tvNoMessages.visibility = View.VISIBLE
            }

        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatActivity", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messagesList.clear()
                    for (doc in snapshot.documents) {
                        val message = doc.toObject(Message::class.java)?.apply {
                            id = doc.id
                        }
                        message?.let { messagesList.add(it) }
                    }

                    messagesAdapter.notifyDataSetChanged()
                    rvMessages.scrollToPosition(messagesList.size - 1)

                    // Mark messages as read
                    markMessagesAsRead()

                    // Hide "no messages" text if we have messages
                    if (messagesList.isNotEmpty()) {
                        tvNoMessages.visibility = View.GONE
                    }
                }
            }
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val currentUser = auth.currentUser ?: return

        etMessage.setText("")

        val message = Message(
            senderId = currentUser.uid,
            text = messageText,
            timestamp = System.currentTimeMillis(),
            read = false
        )

        if (conversationId.isEmpty()) {
            createNewConversation(message)
        } else {
            addMessageToConversation(message)
        }
    }

    private fun createNewConversation(message: Message) {
        val currentUser = auth.currentUser ?: return

        // Create conversation document
        val conversation = hashMapOf(
            "participants" to listOf(currentUser.uid, otherUserId),
            "lastMessage" to message.text,
            "lastMessageTimestamp" to message.timestamp
        )

        firestore.collection("conversations")
            .add(conversation)
            .addOnSuccessListener { documentRef ->
                conversationId = documentRef.id

                // Add the message to the new conversation
                addMessageToConversation(message)
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error creating conversation", e)
            }
    }

    private fun addMessageToConversation(message: Message) {
        // Add message to the conversation
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Update conversation with last message info
                firestore.collection("conversations")
                    .document(conversationId)
                    .update(
                        mapOf(
                            "lastMessage" to message.text,
                            "lastMessageTimestamp" to message.timestamp
                        )
                    )
            }
            .addOnFailureListener { e ->
                Log.e("ChatActivity", "Error sending message", e)
            }
    }

    private fun markMessagesAsRead() {
        val currentUser = auth.currentUser ?: return

        val unreadMessages = messagesList.filter {
            !it.read && it.senderId != currentUser.uid
        }

        if (unreadMessages.isNotEmpty()) {
            val batch = firestore.batch()

            unreadMessages.forEach { message ->
                val messageRef = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(message.id)

                batch.update(messageRef, "read", true)
            }

            batch.commit().addOnFailureListener { e ->
                Log.e("ChatActivity", "Error marking messages as read", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}