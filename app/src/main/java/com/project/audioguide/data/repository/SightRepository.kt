package com.project.audioguide.data.repository

import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.database.sights
import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.domain.service.models.Location
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

// Добавляем api
class SightRepository @Inject constructor(private val dao: SightDao, private val apiService: ApiService) {

    suspend fun replaceAll(sights: List<SightEntity>) {
        dao.replaceAll(sights)
    }

    suspend fun getAll(): List<SightEntity> = dao.getAll()

    suspend fun loadInitialData() {
        if (dao.getAll().isEmpty()) {
            dao.insertAll(sights)
        }
    }

    // метод загрузки с сервера с обработкой ошибок и сохранением кэша
    suspend fun refreshFromServer(): Result<Unit> {
        return try {
            val remoteSights = apiService.loadSights()
            val entities = remoteSights.map { response ->
                SightEntity(
                    sightId = 0,
                    address = "test",
                    location = Location(
                        latitude = response.latitude,
                        longitude = response.longitude
                    ),
                    name = response.name,
                    description = response.description.orEmpty()
                )
            }
            dao.replaceAll(entities)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}