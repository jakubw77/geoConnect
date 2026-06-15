package com.example.data

data class Friend(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val lat: Double,
    val lng: Double,
    val isSharing: Boolean = true,
    
    // Calculated live relative properties
    val linearDistanceMeters: Double? = null,
    val drivingDistanceText: String? = null,
    val drivingDurationText: String? = null,
    val polylinePoints: String? = null,
    val steps: List<String>? = null
)
