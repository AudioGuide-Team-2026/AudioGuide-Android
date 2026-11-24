package com.project.audioguide.domain.repository

import com.project.audioguide.domain.model.Sight
import kotlinx.coroutines.flow.Flow

interface SightRepository {
    fun observeAll(): Flow<List<Sight>>
    suspend fun getAll(): List<Sight>
    suspend fun loadInitialData()
    suspend fun refreshFromServer(): Result<Unit>
}