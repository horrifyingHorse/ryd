package com.example.ryd

data class Conversation(
    var id: String = "",
    val participants: List<String> = listOf(),
    var lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    var otherUserName: String = "",
    var otherUserPhoto: String = "",
    val ride: Map<String, String> = mapOf()
)