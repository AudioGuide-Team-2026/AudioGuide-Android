package com.project.audioguide.domain.service

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationService {
    fun getLocation(): Flow<Location>
}
