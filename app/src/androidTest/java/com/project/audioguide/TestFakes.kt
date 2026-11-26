package com.project.audioguide

import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import com.project.audioguide.domain.repository.SightRepository
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Репозиторий-заглушка: данные и результат обновления задаются из теста. */
@Singleton
class FakeSightRepository @Inject constructor() : SightRepository {
    val sights = MutableStateFlow<List<Sight>>(emptyList())
    var refreshResult: Result<Unit> = Result.success(Unit)
    @Volatile var refreshCalls = 0

    /** Если задан, refreshFromServer ждет, пока тест его не завершит. */
    @Volatile var refreshGate: CompletableDeferred<Unit>? = null

    override fun observeAll(): Flow<List<Sight>> = sights

    override suspend fun getAll(): List<Sight> = sights.value

    override suspend fun loadInitialData() {}

    override suspend fun refreshFromServer(): Result<Unit> {
        refreshCalls++
        refreshGate?.await()
        return refreshResult
    }
}

@Singleton
class FakeLocationService @Inject constructor() : LocationService {
    val location = MutableStateFlow(Location(0.0, 0.0))
    override fun getLocation(): Flow<Location> = location
}

@Singleton
class FakeCompassService @Inject constructor() : CompassService {
    val azimuth = MutableStateFlow(0f)
    override fun getUserAzimuth(): Flow<Float> = azimuth
}
