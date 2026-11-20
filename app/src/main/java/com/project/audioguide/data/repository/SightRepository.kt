package com.project.audioguide.data.repository

import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.database.sights
import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.domain.service.models.Location
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
    // метод загрузки с сервера
    suspend fun refreshFromServer() {
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
    }
}