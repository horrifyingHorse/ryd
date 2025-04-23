package com.example.ryd

data class Ride(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String = "",
    val fromLocation: String = "",
    val destination: String = "",
    val departureTime: Long = 0,
    val isDriver: Boolean = false,
    val seats: Int = 0,
    val description: String = "",
    val timestamp: Long = 0,
    val userDepartment: String = "",
    val userYear: String = "",
    var status: String = "upcoming"
)