package com.project.audioguide.data.database

import com.project.audioguide.domain.repository.SightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Говорим Hilt, что этот класс нужно создать один раз
class DatabaseInitializer @Inject constructor(
    private val repository: SightRepository // Hilt сам даст правильный репозиторий (Сейф Б)
) {
    // Сохраняем работу в фоновом потоке (Dispatchers.IO)
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Заполняем базу начальными данными, если она ещё пуста
        repository.loadInitialData()
    }
}