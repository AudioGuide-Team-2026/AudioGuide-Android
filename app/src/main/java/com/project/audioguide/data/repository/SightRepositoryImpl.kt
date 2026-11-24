package com.project.audioguide.data.repository

import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.database.sights
import com.project.audioguide.data.database.toDomain
import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import com.project.audioguide.domain.repository.SightRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Добавляем api
class SightRepositoryImpl @Inject constructor(
    private val dao: SightDao,
    private val apiService: ApiService
) : SightRepository {

    override fun observeAll(): Flow<List<Sight>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Sight> =
        dao.getAll().map { it.toDomain() }

    override suspend fun loadInitialData() {
        if (dao.getAll().isEmpty()) {
            dao.insertAll(sights)
        }
    }

    // метод загрузки с сервера с обработкой ошибок и сохранением кэша
    override suspend fun refreshFromServer(): Result<Unit> {
        return try {
            val remoteSights = apiService.loadSights()
            val entities = remoteSights.map { response ->
                SightEntity(
                    sightId = 0,
                    address = response.address,
                    location = Location(
                        latitude = response.location.latitude,
                        longitude = response.location.longitude
                    ),
                    name = response.name,
                    description = response.description
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