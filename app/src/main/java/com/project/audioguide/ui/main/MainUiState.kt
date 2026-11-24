package com.project.audioguide.ui.main

import com.project.audioguide.domain.model.Sight

data class MainUiState(
    val title: String = "Text",
    val arrowAngle: Float = 0f,
    val isLoading: Boolean = false,
    val statusMessage: String = "Поиск...",
    val detectedSight: Sight? = null,
    val message: String? = null
)