package com.example.pict

data class ApiResponse(
    val answer: String,
    val status: String = "success",
    val error: String? = null
) 