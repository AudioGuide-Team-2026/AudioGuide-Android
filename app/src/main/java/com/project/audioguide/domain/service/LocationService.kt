package com.project.audioguide.domain.service

import com.project.audioguide.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun getLocation(): Flow<Location>
}