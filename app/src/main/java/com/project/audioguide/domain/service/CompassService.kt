package com.project.audioguide.domain.service

import kotlinx.coroutines.flow.Flow

interface CompassService {
    fun getUserAzimuth(): Flow<Float>
}
