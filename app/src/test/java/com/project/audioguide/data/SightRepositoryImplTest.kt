package com.project.audioguide.data

import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.database.sights
import com.project.audioguide.data.remote.response.LocationResponse
import com.project.audioguide.data.remote.response.SightEntityResponse
import com.project.audioguide.data.repository.SightRepositoryImpl
import com.project.audioguide.domain.model.Location
import com.project.audioguide.fakes.FakeApiService
import com.project.audioguide.fakes.FakeSightDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SightRepositoryImplTest {

    private lateinit var dao: FakeSightDao
    private lateinit var api: FakeApiService
    private lateinit var repository: SightRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeSightDao()
        api = FakeApiService()
        repository = SightRepositoryImpl(dao, api)
    }

    private fun remote(name: String, lat: Double = 1.0, lon: Double = 2.0) = SightEntityResponse(
        id = name,
        address = "Адрес $name",
        location = LocationResponse(lat, lon),
        name = name,
        description = "Описание $name"
    )

    // Требование 1: работа без интернета (данные из локальной БД)

    @Test
    fun loadInitialData_emptyDatabase_fillsDefaultSights() = runTest {
        repository.loadInitialData()

        assertEquals(sights.size, repository.getAll().size)
    }

    @Test
    fun loadInitialData_nonEmptyDatabase_doesNotDuplicate() = runTest {
        repository.loadInitialData()
        repository.loadInitialData()

        assertEquals(sights.size, repository.getAll().size)
    }

    @Test
    fun getAll_withoutNetwork_returnsLocalData() = runTest {
        api.onLoad = { throw IOException("Нет сети") }
        repository.loadInitialData()

        val result = repository.getAll()

        assertEquals(sights.map { it.name }, result.map { it.name })
        assertEquals(0, api.calls)
    }

    @Test
    fun getAll_mapsEntityToDomain() = runTest {
        dao.insert(
            SightEntity(
                address = "Адрес",
                location = Location(59.9, 30.3),
                name = "Объект",
                description = "Текст"
            )
        )

        val sight = repository.getAll().single()

        assertEquals(1, sight.id)
        assertEquals("Адрес", sight.address)
        assertEquals(Location(59.9, 30.3), sight.location)
        assertEquals("Объект", sight.name)
        assertEquals("Текст", sight.description)
    }

    @Test
    fun observeAll_emitsCurrentData() = runTest {
        repository.loadInitialData()

        val emitted = repository.observeAll().first()

        assertEquals(sights.size, emitted.size)
    }

    // Требование 2: обновление данных по сети

    @Test
    fun refreshFromServer_success_replacesLocalData() = runTest {
        repository.loadInitialData()
        api.onLoad = { listOf(remote("Новый 1", 10.0, 20.0), remote("Новый 2")) }

        val result = repository.refreshFromServer()

        assertTrue(result.isSuccess)
        val all = repository.getAll()
        assertEquals(listOf("Новый 1", "Новый 2"), all.map { it.name })
        assertEquals(Location(10.0, 20.0), all[0].location)
    }

    @Test
    fun refreshFromServer_success_resetsIdSequence() = runTest {
        repository.loadInitialData()
        api.onLoad = { listOf(remote("A"), remote("B")) }

        repository.refreshFromServer()

        assertEquals(listOf(1, 2), repository.getAll().map { it.id })
    }

    @Test
    fun refreshFromServer_emptyResponse_clearsData() = runTest {
        repository.loadInitialData()
        api.onLoad = { emptyList() }

        val result = repository.refreshFromServer()

        assertTrue(result.isSuccess)
        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun refreshFromServer_networkError_returnsFailureAndKeepsCache() = runTest {
        repository.loadInitialData()
        val before = repository.getAll()
        api.onLoad = { throw IOException("Нет сети") }

        val result = repository.refreshFromServer()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(before, repository.getAll())
    }

    @Test
    fun refreshFromServer_cancellation_isRethrown() = runTest {
        api.onLoad = { throw CancellationException("cancel") }

        try {
            repository.refreshFromServer()
            fail("CancellationException должен пробрасываться дальше")
        } catch (e: CancellationException) {
            // ожидаемо
        }
    }

    @Test
    fun refreshFromServer_callsApiOnce() = runTest {
        api.onLoad = { listOf(remote("A")) }

        repository.refreshFromServer()

        assertEquals(1, api.calls)
        assertFalse(repository.getAll().isEmpty())
    }
}
