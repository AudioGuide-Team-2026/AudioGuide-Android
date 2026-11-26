package com.project.audioguide.data

import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.data.repository.SightRepositoryImpl
import com.project.audioguide.domain.model.Location
import com.project.audioguide.fakes.FakeSightDao
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Проверка работы с сервером без реального бэкенда:
 * поддельный сервер отвечает JSON в формате из README AudioGuide-Backend.
 */
class ApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    private val twoPlacesJson = """
        [
          {
            "id": "11111111-1111-1111-1111-111111111111",
            "address": "Кронверкский пр., 49",
            "location": { "latitude": 59.956363, "longitude": 30.310011 },
            "name": "Главный корпус ИТМО",
            "description": "Главное здание университета на Петроградской стороне"
          },
          {
            "id": "22222222-2222-2222-2222-222222222222",
            "address": "ул. Ломоносова, 9",
            "location": { "latitude": 59.927288, "longitude": 30.338353 },
            "name": "Корпус на Ломоносова",
            "description": ""
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        // Некоторые тесты уже останавливают сервер сами, повторная остановка может бросить исключение
        try {
            server.shutdown()
        } catch (e: Exception) {
            // ничего страшного
        }
    }

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    // ApiService: запрос и разбор ответа

    @Test
    fun loadSights_sendsGetRequestToPlacesEndpoint() = runTest {
        enqueueJson("[]")

        api.loadSights()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/places", request.path)
    }

    @Test
    fun loadSights_parsesAllFields() = runTest {
        enqueueJson(twoPlacesJson)

        val places = api.loadSights()

        assertEquals(2, places.size)
        val first = places[0]
        assertEquals("11111111-1111-1111-1111-111111111111", first.id)
        assertEquals("Кронверкский пр., 49", first.address)
        assertEquals(59.956363, first.location.latitude, 0.000001)
        assertEquals(30.310011, first.location.longitude, 0.000001)
        assertEquals("Главный корпус ИТМО", first.name)
        assertEquals("Главное здание университета на Петроградской стороне", first.description)
    }

    @Test
    fun loadSights_emptyDescription_isEmptyString() = runTest {
        enqueueJson(twoPlacesJson)

        val places = api.loadSights()

        assertEquals("", places[1].description)
    }

    @Test
    fun loadSights_emptyList_returnsEmptyList() = runTest {
        enqueueJson("[]")

        assertTrue(api.loadSights().isEmpty())
    }

    @Test(expected = HttpException::class)
    fun loadSights_serverError_throwsHttpException() = runTest {
        enqueueJson("{}", code = 500)

        api.loadSights()
    }

    @Test(expected = IOException::class)
    fun loadSights_serverUnavailable_throwsIOException() = runTest {
        server.shutdown()

        api.loadSights()
    }

    // Репозиторий вместе с API: полный путь "сервер -> локальная база"

    @Test
    fun repository_refreshFromServer_savesServerDataToDatabase() = runTest {
        enqueueJson(twoPlacesJson)
        val repository = SightRepositoryImpl(FakeSightDao(), api)

        val result = repository.refreshFromServer()

        assertTrue(result.isSuccess)
        val saved = repository.getAll()
        assertEquals(listOf("Главный корпус ИТМО", "Корпус на Ломоносова"), saved.map { it.name })
        assertEquals(Location(59.956363, 30.310011), saved[0].location)
        assertEquals("Главное здание университета на Петроградской стороне", saved[0].description)
    }

    @Test
    fun repository_serverError_keepsOldDataAndReturnsFailure() = runTest {
        val dao = FakeSightDao()
        val repository = SightRepositoryImpl(dao, api)
        enqueueJson(twoPlacesJson)
        repository.refreshFromServer()
        val before = repository.getAll()

        enqueueJson("{}", code = 500)
        val result = repository.refreshFromServer()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
        assertEquals(before, repository.getAll())
    }

    @Test
    fun repository_serverUnavailable_keepsOldDataAndReturnsFailure() = runTest {
        val repository = SightRepositoryImpl(FakeSightDao(), api)
        enqueueJson(twoPlacesJson)
        repository.refreshFromServer()
        val before = repository.getAll()

        server.shutdown()
        val result = repository.refreshFromServer()

        assertTrue(result.isFailure)
        assertEquals(before, repository.getAll())
    }
}
