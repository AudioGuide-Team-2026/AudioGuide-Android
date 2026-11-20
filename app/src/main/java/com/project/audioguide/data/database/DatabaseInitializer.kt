package com.project.audioguide.data.database

import com.project.audioguide.data.repository.SightRepository
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
        // Проверяем, не заполнена ли уже база, чтобы не перезаписывать каждый раз
        if (repository.getAll().isEmpty()) {
            repository.replaceAll(sights)
            android.util.Log.d("DB_TEST", "База успешно заполнена! Объектов: ${sights.size}")
        } else {
            android.util.Log.d("DB_TEST", "База уже содержит данные.")
        }
    }
}