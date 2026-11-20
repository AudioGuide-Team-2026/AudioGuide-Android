package com.project.audioguide.ui.main

data class MainUiState(
    val title: String = "Text",
    val arrowAngle: Float = 0f,
    val isLoading: Boolean = false,
    val statusMessage: String = "Поиск...",
    val detectedSight: com.project.audioguide.data.database.SightEntity? = null
)