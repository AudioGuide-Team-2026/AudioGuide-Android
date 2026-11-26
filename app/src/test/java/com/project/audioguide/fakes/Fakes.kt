package com.project.audioguide.fakes

import com.project.audioguide.data.database.SightDao
import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.data.remote.response.SightEntityResponse
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import com.project.audioguide.domain.repository.SightRepository
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** DAO в памяти: ведет себя как Room (автоинкремент id, сброс счетчика). */
class FakeSightDao : SightDao {
    private val state = MutableStateFlow<List<SightEntity>>(emptyList())
    private var nextId = 1

    override suspend fun insert(sight: SightEntity) = insertAll(listOf(sight))

    override suspend fun insertAll(sights: List<SightEntity>) {
        val added = sights.map { it.copy(sightId = nextId++) }
        state.value = state.value + added
    }

    override fun observeAll(): Flow<List<SightEntity>> = state

    override suspend fun getAll(): List<SightEntity> = state.value

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    override suspend fun resetSequence() {
        nextId = 1
    }
}

/** API-заглушка: поведение задается из теста. */
class FakeApiService : ApiService {
    var onLoad: suspend () -> List<SightEntityResponse> = { emptyList() }
    var calls = 0

    override suspend fun loadSights(): List<SightEntityResponse> {
        calls++
        return onLoad()
    }
}

/** Репозиторий-заглушка для тестов ViewModel. */
class FakeSightRepository(initial: List<Sight> = emptyList()) : SightRepository {
    val sights = MutableStateFlow(initial)
    var loadInitialDataCalls = 0
    var refreshResult: Result<Unit> = Result.success(Unit)

    /** Если задан, refreshFromServer будет ждать, пока тест его не завершит. */
    var refreshGate: CompletableDeferred<Unit>? = null

    override fun observeAll(): Flow<List<Sight>> = sights

    override suspend fun getAll(): List<Sight> = sights.value

    override suspend fun loadInitialData() {
        loadInitialDataCalls++
    }

    override suspend fun refreshFromServer(): Result<Unit> {
        refreshGate?.await()
        return refreshResult
    }
}

class FakeLocationService : LocationService {
    val location = MutableStateFlow(Location(0.0, 0.0))
    override fun getLocation(): Flow<Location> = location
}

class FakeCompassService : CompassService {
    val azimuth = MutableStateFlow(0f)
    override fun getUserAzimuth(): Flow<Float> = azimuth
}
