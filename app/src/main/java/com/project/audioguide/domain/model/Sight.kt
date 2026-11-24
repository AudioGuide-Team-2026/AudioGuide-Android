package com.project.audioguide.domain.model

data class Sight(
    val id: Int = 0,
    val address: String,
    val location: Location,
    val name: String,
    val description: String
)