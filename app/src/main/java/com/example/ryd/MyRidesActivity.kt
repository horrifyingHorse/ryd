package com.example.ryd

import android.os.Bundle
import android.view.View
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

        val query = if (isActive) {
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

        query.get()
            .addOnSuccessListener { documents ->
                ridesList.clear()

                for (document in documents) {
                    val ride = document.toObject(Ride::class.java).apply {
                        id = document.id
                    }
                    ridesList.add(ride)
                }

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
                showNoRides("Error loading rides: ${e.message}")
            }
    }

    private fun showNoRides(message: String) {
        tvNoRides.text = message
        tvNoRides.visibility = View.VISIBLE
        rvMyRides.visibility = View.GONE
    }

    private fun onRideSelected(ride: Ride) {
        // Handle ride selection to edit, cancel, or view details
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}