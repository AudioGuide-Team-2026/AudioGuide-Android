package com.project.audioguide.data.models
import com.project.audioguide.domain.service.models.Location

data class Sight (
    val address: String,
    val location: Location,
    val name: String,
    val description: String,
)