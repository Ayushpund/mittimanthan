package com.example.pict

data class ChatMessage(
    val text: String,
    val isBot: Boolean,
    val isLoading: Boolean = false
)