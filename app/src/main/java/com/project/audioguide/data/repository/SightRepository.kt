package com.project.audioguide.data.repository

import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightEntity

class SightRepository(private val dao: SightDao) {

    suspend fun replaceAll(sights: List<SightEntity>) {
        dao.insertAll(sights)
    }

    suspend fun getAll(): List<SightEntity> = dao.getAll()
}