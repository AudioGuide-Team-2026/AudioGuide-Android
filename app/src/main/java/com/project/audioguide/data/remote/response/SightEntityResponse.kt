package com.project.audioguide.data.remote.response

import kotlinx.serialization.Serializable

data class SightEntityResponse(
    val id: String,
    val address: String,
    val location: LocationResponse,
    val name: String,
    val description: String
)