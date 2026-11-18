package com.project.audioguide.data.remote.response

import kotlinx.serialization.Serializable

@Serializable
data class SightEntityResponse(
    val id: Int,
    val address: String,
    val location: LocationResponse,
    val name: String,
    val description: String
)