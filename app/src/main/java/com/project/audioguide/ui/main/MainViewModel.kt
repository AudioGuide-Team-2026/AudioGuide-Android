package com.project.audioguide.ui.main

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.audioguide.data.database.SightEntity
import com.project.audioguide.data.database.sights // Импорт файла с достопримечательностями
import com.project.audioguide.data.repository.SightRepository // Импорт репозитория
import com.project.audioguide.domain.SightDetector // Добавили
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationService: LocationService,
    private val compassService: CompassService,
    private val repository: SightRepository // Внедряем репозиторий для БД
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Наш "мозг" для математики
    private val detector = SightDetector()

    // Текущие данные от датчиков
    private var currentLocation: Location? = null
    private var currentAzimuth: Float = 0f

    private var isTrackingStarted = false

    init {
        loadInitialData()
    }

    fun startTracking() {
        if (isTrackingStarted) return
        isTrackingStarted = true

        startLocationTracking()
        startCompassTracking()
        startPeriodicChecking()
    }

    private fun startLocationTracking() {
        locationService.getLocation()
            .onEach { location ->
                currentLocation = location // Просто обновляем переменную
            }
            .catch { e ->
                isTrackingStarted = false
                Log.e(TAG, "Ошибка GPS", e)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Ошибка GPS: ${e.message}"
                )
            }
            .launchIn(viewModelScope)
    }

    private fun startCompassTracking() {
        compassService.getUserAzimuth()
            .onEach { azimuth ->
                currentAzimuth = azimuth // Просто обновляем переменную
                _uiState.value = _uiState.value.copy(arrowAngle = azimuth)
            }
            .catch { e ->
                Log.e(TAG, "Ошибка компаса", e)
            }
            .launchIn(viewModelScope)
    }

    // Таймер, который раз в секунду проверяет обстановку
    private fun startPeriodicChecking() {
        viewModelScope.launch {
            while (true) {
                // Проверяем раз в 1 секунду (можно поставить 500мс для плавности)
                delay(1000L)
                checkSurroundings()
            }
        }
    }

    // Твой алгоритм (немного изменен, теперь это suspend-функция)
    private suspend fun checkSurroundings() {
        val location = currentLocation ?: return // Ждем GPS

        // 1. Достаем все объекты из базы
        val allSights = repository.getAll()

        // Если база все еще пуста (вдруг не успела), просто ждем следующего тика
        if (allSights.isEmpty()) {
            Log.d(TAG, "База данных пуста, ждем загрузки...")
            return
        }

        var nearestSight: SightEntity? = null
        var minDistance = Float.MAX_VALUE

        // 2. Ищем ближайший объект в радиусе 50 метров
        for (sight in allSights) {
            val dist = detector.calculateDistance(
                location.latitude, location.longitude,
                sight.location.latitude, sight.location.longitude
            )
            if (dist <= 50f && dist < minDistance) {
                nearestSight = sight
                minDistance = dist
            }
        }

        // 3. Если ничего нет рядом
        if (nearestSight == null) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "В радиусе 50 метров ничего нет.",
                detectedSight = null
            )
            return
        }

        // 4. Проверяем угол
        val isLooking = detector.isLookingAtSight(
            location.latitude, location.longitude, currentAzimuth,
            nearestSight.location.latitude, nearestSight.location.longitude
        )

        // 5. Обновляем UI
        if (isLooking) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Вы смотрите на: ${nearestSight.name}",
                detectedSight = nearestSight
            )
        } else {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Рядом: ${nearestSight.name}. Повернитесь к нему!",
                detectedSight = null
            )
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            try {
                repository.loadInitialData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ошибки при начальной загрузке — игнорируем, данные уже есть или нет
            }
        }
    }

    // обновляем метод для загрузки с сервера
    fun reloadSightsFromServer() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Загрузка с сервера...")
            repository.refreshFromServer()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Данные обновлены"
                    )
                }
                .onFailure { e ->
                    // Сохраняем имеющийся кэш, показываем ошибку
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Ошибка обновления: ${e.message}"
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun reloadSightsFromSource() {
        viewModelScope.launch {
            repository.replaceAll(sights) // Запись в базу данных
        }
    }
}