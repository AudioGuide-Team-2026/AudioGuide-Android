package com.project.audioguide.data.remote.response

import kotlinx.serialization.Serializable

data class SightEntityResponse(
    val id: String,
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double
)