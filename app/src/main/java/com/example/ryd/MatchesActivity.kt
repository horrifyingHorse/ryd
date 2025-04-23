package com.example.ryd

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ryd.HomeActivity.RideMatch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MatchesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoMatches: TextView
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var pgBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matches)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Matched Rides"

        // Initialize views
        recyclerView = findViewById(R.id.rvMatches)
        tvNoMatches = findViewById(R.id.tvNoMatches)
        pgBar = findViewById(R.id.progressBar)

        pgBar.visibility = View.VISIBLE

        // Get matches from intent
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("matches", RideMatch::class.java) ?: ArrayList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<RideMatch>("matches") ?: ArrayList()
        }

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        matchAdapter = MatchAdapter(matches, this::onRideSelected)
        recyclerView.adapter = matchAdapter

        pgBar.visibility = View.GONE

        // Show/hide no matches view
        if (matches.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoMatches.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoMatches.visibility = View.GONE
        }
    }

    private fun onRideSelected(rideMatch: RideMatch) {
        // Open ride details
        val intent = Intent(this, RideDetailActivity::class.java)
        intent.putExtra("rideId", rideMatch.ride.id)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Adapter for matches