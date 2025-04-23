package com.example.ryd

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import kotlin.text.clear
import kotlin.text.get
import kotlin.text.toInt

class MyRidesActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvMyRides: RecyclerView
    private lateinit var tvNoRides: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val ridesList = mutableListOf<Ride>()
    private lateinit var ridesAdapter: MyRidesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_rides)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvMyRides = findViewById(R.id.rvMyRides)
        tvNoRides = findViewById(R.id.tvNoRides)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Rides"

        // Setup RecyclerView
        rvMyRides.layoutManager = LinearLayoutManager(this)
        ridesAdapter = MyRidesAdapter(ridesList, this::onRideSelected)
        rvMyRides.adapter = ridesAdapter

        // Setup tabs
        setupTabs()

        // Load active rides by default
        loadRides(isActive = true)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Active"))
        tabLayout.addTab(tabLayout.newTab().setText("Past"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> loadRides(isActive = true)
                    1 -> loadRides(isActive = false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadRides(isActive: Boolean) {
        val currentUser = auth.currentUser ?: return
        val currentTime = System.currentTimeMillis()
        val pgBar = findViewById<ProgressBar>(R.id.progressBar)

        pgBar.visibility = View.VISIBLE
        ridesList.clear()

        val postedRidesQuery = if (isActive) {
            firestore.collection("rides")
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThan("departureTime", currentTime)
                .orderBy("departureTime", Query.Direction.ASCENDING)
        } else {
            firestore.collection("rides")
                .whereEqualTo("userId", currentUser.uid)
                .whereLessThan("departureTime", currentTime)
                .orderBy("departureTime", Query.Direction.DESCENDING)
        }

        postedRidesQuery.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val ride = document.toObject(Ride::class.java).apply {
                        id = document.id
                        status = "posted" // Mark as posted by this user
                    }
                    ridesList.add(ride)
                }

                // Then load the rides the user has requested/joined
                loadUserRequestedRides(isActive, currentTime)
            }
            .addOnFailureListener { e ->
                pgBar.visibility = View.GONE
                showNoRides("Error loading rides: ${e.message}")
                Log.e("MyRidesActivity", "Error loading rides", e)
            }
    }

    private fun loadUserRequestedRides(isActive: Boolean, currentTime: Long) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("userRides")
            .document(currentUser.uid)
            .collection("rides")
            .get()
            .addOnSuccessListener { documents ->
                val pgBar = findViewById<ProgressBar>(R.id.progressBar)

                for (document in documents) {
                    val departureTime = document.getLong("departureTime") ?: 0

                    // Filter based on whether we're looking at active or past rides
                    if ((isActive && departureTime > currentTime) ||
                        (!isActive && departureTime <= currentTime)) {

                        val ride = Ride(
                            id = document.getString("originalRideId") ?: "",
                            userId = document.getString("ridePosterUserId") ?: "",
                            userName = document.getString("ridePosterName") ?: "",
                            fromLocation = document.getString("fromLocation") ?: "",
                            destination = document.getString("destination") ?: "",
                            departureTime = departureTime,
                            isDriver = document.getBoolean("isDriver") ?: false,
                            seats = document.getLong("seats")?.toInt() ?: 0,
                            description = document.getString("description") ?: ""
                        ).apply {
                            status = document.getString("status") ?: "requested"
                        }
                        ridesList.add(ride)
                    }
                }

                // Now update the UI
                pgBar.visibility = View.GONE
                if (ridesList.isEmpty()) {
                    val message = if (isActive) "You have no active rides" else "You have no past rides"
                    showNoRides(message)
                } else {
                    tvNoRides.visibility = View.GONE
                    rvMyRides.visibility = View.VISIBLE
                    ridesAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                val pgBar = findViewById<ProgressBar>(R.id.progressBar)
                pgBar.visibility = View.GONE
                Log.e("MyRidesActivity", "Error loading requested rides", e)
            }
    }

    private fun showNoRides(message: String) {
        tvNoRides.text = message
        tvNoRides.visibility = View.VISIBLE
        rvMyRides.visibility = View.GONE
    }

    private fun onRideSelected(ride: Ride) {
        // Open RideDetailActivity for the selected ride
        Log.d("Ride:(", "The ride id is ${ride.id}")
        val intent = Intent(this, RideDetailActivity::class.java).apply {
            putExtra("rideId", ride.id)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun refreshRides() {
        val currentTab = tabLayout.selectedTabPosition
        loadRides(isActive = currentTab == 0)
    }
}