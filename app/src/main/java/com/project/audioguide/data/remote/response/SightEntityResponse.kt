package com.project.audioguide.data.remote.response

data class SightEntityResponse(
    val id: String,
    val address: String,
    val location: LocationResponse,
    val name: String,
    val description: String
)