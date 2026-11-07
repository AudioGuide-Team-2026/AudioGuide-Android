package com.project.audioguide.data.database

import android.content.Context
import com.project.audioguide.data.repository.SightRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseInitializer(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = SightDatabase.getInstance(context).sightDao()
    private val repository = SightRepository(dao)

    suspend fun initialize() = withContext(dispatcher) {
        repository.replaceAll(sights)
    }
}