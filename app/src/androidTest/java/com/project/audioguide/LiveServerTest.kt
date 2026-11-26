
package com.project.audioguide

import com.project.audioguide.data.remote.ApiService
import com.project.audioguide.data.remote.response.SightEntityResponse
import android.os.Build
import android.os.SystemClock
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Проверка с НАСТОЯЩИМ локальным сервером (AudioGuide-Backend).
 * Перед запуском бэкенд должен работать на компьютере (порт 5213), а тест идти на эмуляторе:
 * эмулятор видит компьютер по адресу 10.0.2.2.
 * Если сервер не запущен, тесты пропускаются, а не падают.
 */
class LiveServerTest {

    // Android 17+: без этого разрешения доступ к локальной сети (10.0.2.2) блокируется
    @get:Rule
    val localNetworkRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 37) {
            GrantPermissionRule.grant("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            GrantPermissionRule.grant()
        }

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5213/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun loadOrSkip(): List<SightEntityResponse> {
        return try {
            runBlocking { api.loadSights() }
        } catch (e: IOException) {
            assumeNoException("Сервер недоступен, запусти бэкенд на порту 5213", e)
            emptyList()
        }
    }

    @Test
    fun server_returnsNonEmptyListOfPlaces() {
        val places = loadOrSkip()

        assertTrue("Сервер вернул пустой список мест", places.isNotEmpty())
    }

    @Test
    fun server_placesHaveValidFields() {
        val places = loadOrSkip()

        places.forEach { place ->
            assertTrue("Пустой id", place.id.isNotBlank())
            assertTrue("Пустое название, id=${place.id}", place.name.isNotBlank())
            assertTrue("Некорректная широта у ${place.name}", place.location.latitude in -90.0..90.0)
            assertTrue("Некорректная долгота у ${place.name}", place.location.longitude in -180.0..180.0)
        }
    }

    // Нефункциональное требование: обновление данных не дольше 5 секунд на каждую запись
    @Test
    fun server_updateTakesLessThanFiveSecondsPerRecord() {
        val start = SystemClock.elapsedRealtime()
        val places = loadOrSkip()
        val elapsed = SystemClock.elapsedRealtime() - start

        assertTrue("Сервер вернул пустой список мест", places.isNotEmpty())
        val perRecord = elapsed.toDouble() / places.size
        assertTrue("На запись ушло $perRecord мс, ожидалось меньше 5000", perRecord < 5000)
    }
}
