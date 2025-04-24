package com.example.ryd

data class Message(
    var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false
)