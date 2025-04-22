package com.example.ryd

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MatchesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var rvMatches: RecyclerView
    private lateinit var tvNoMatches: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val matchesList = mutableListOf<Ride>()
    private lateinit var matchAdapter: MatchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_matches)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        rvMatches = findViewById(R.id.rvMatches)
        tvNoMatches = findViewById(R.id.tvNoMatches)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Potential Matches"

        // Setup RecyclerView
        rvMatches.layoutManager = LinearLayoutManager(this)
        matchAdapter = MatchAdapter(matchesList, this::onMatchSelected)
        rvMatches.adapter = matchAdapter

        // Load matches
        loadMatches()
    }

    private fun loadMatches() {
        val currentUser = auth.currentUser ?: return

        // Find user's rides first
        firestore.collection("rides")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { myRidesSnapshot ->
                if (myRidesSnapshot.isEmpty) {
                    showNoMatches("You haven't created any rides yet")
                    return@addOnSuccessListener
                }

                // Get the most recent ride
                val myRide = myRidesSnapshot.documents
                    .mapNotNull { it.toObject(Ride::class.java)?.apply { id = it.id } }
                    .maxByOrNull { it.timestamp } ?: return@addOnSuccessListener

                // Find matching rides
                val matchQuery = if (myRide.isDriver) {
                    // I'm a driver, find passengers going to the same destination
                    firestore.collection("rides")
                        .whereEqualTo("destination", myRide.destination)
                        .whereEqualTo("isDriver", false)
                } else {
                    // I'm a passenger, find drivers going to the same destination
                    firestore.collection("rides")
                        .whereEqualTo("destination", myRide.destination)
                        .whereEqualTo("isDriver", true)
                }

                matchQuery.get()
                    .addOnSuccessListener { matchesSnapshot ->
                        matchesList.clear()

                        for (document in matchesSnapshot.documents) {
                            val matchRide = document.toObject(Ride::class.java)?.apply { id = document.id }
                            if (matchRide != null && matchRide.userId != currentUser.uid) {
                                // Check if departure times are reasonably close (within 30 minutes)
                                if (Math.abs(matchRide.departureTime - myRide.departureTime) <= 30 * 60 * 1000) {
                                    matchesList.add(matchRide)
                                }
                            }
                        }

                        if (matchesList.isEmpty()) {
                            showNoMatches("No matches found for your ride to ${myRide.destination}")
                        } else {
                            tvNoMatches.visibility = View.GONE
                            rvMatches.visibility = View.VISIBLE
                            matchAdapter.notifyDataSetChanged()
                        }
                    }
                    .addOnFailureListener { e ->
                        showNoMatches("Error finding matches: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showNoMatches("Error loading your rides: ${e.message}")
            }
    }

    private fun showNoMatches(message: String) {
        tvNoMatches.text = message
        tvNoMatches.visibility = View.VISIBLE
        rvMatches.visibility = View.GONE
    }

    private fun onMatchSelected(ride: Ride) {
        // Handle match selection - start conversation or show details
        // Implementation will depend on your app's flow
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}