package com.example.ryd

data class RideRequest(
    var id: String = "",
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val driverId: String = "",
    val status: String = "pending",
    val timestamp: Long = 0,
    val fromLocation: String = "",
    val destination: String = ""
)