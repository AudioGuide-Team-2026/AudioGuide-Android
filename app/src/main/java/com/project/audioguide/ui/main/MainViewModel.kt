package com.project.audioguide.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.audioguide.domain.DetectionResult
import com.project.audioguide.domain.SightDetector // Добавили
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.repository.SightRepository // Импорт интерфейса репозитория из domain
import com.project.audioguide.domain.service.CompassService
import com.project.audioguide.domain.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationService: LocationService,
    private val compassService: CompassService,
    private val repository: SightRepository, // Внедряем интерфейс репозитория для БД
    private val detector: SightDetector    // Наш "мозг" для математики (внедряется через Hilt)
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Одноразовые события для UI (например, Toast)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var trackingJob: Job? = null

    init {
        loadInitialData()
    }

    // Запускаем сбор датчиков и базы реактивно после выдачи разрешений GPS
    fun startTracking() {
        if (trackingJob?.isActive == true) return

        trackingJob = viewModelScope.launch {
            val locationFlow = locationService.getLocation()
                .map<Location, Location?> { it }
                .onStart { emit(null) } // Чтобы combine не ждал первый GPS-фикс для обновления компаса
                .catch { e ->
                    Log.e(TAG, "Ошибка GPS", e)
                    _uiState.update { it.copy(statusMessage = "Ошибка GPS: ${e.message}") }
                    trackingJob?.cancel()
                    trackingJob = null
                }

            val azimuthFlow = compassService.getUserAzimuth()
                .onStart { emit(0f) } // Чтобы при отсутствии датчика компаса GPS и БД всё равно работали
                .catch { e ->
                    Log.e(TAG, "Ошибка компаса", e)
                }

            // Реактивно реагируем на обновление GPS, компаса или данных в БД
            combine(
                locationFlow,
                azimuthFlow,
                repository.observeAll()
            ) { location, azimuth, sights ->
                Triple(location, azimuth, sights)
            }.collect { (location, azimuth, sights) ->
                if (location == null) {
                    _uiState.update { it.copy(arrowAngle = azimuth) }
                    return@collect
                }

                // Если база все еще пуста, просто ждем обновления Room
                if (sights.isEmpty()) {
                    Log.d(TAG, "База данных пуста, ждем загрузки...")
                    _uiState.update {
                        it.copy(
                            arrowAngle = azimuth,
                            statusMessage = "База объектов пуста",
                            detectedSight = null
                        )
                    }
                    return@collect
                }

                // Ищем ближайший объект в радиусе 50 метров, проверяем угол и обновляем UI
                when (val result = detector.detect(location, azimuth, sights)) {
                    // Если ничего нет рядом
                    DetectionResult.NothingNearby -> {
                        _uiState.update {
                            it.copy(
                                arrowAngle = azimuth,
                                statusMessage = "В радиусе 50 метров ничего нет.",
                                detectedSight = null
                            )
                        }
                    }
                    // Объект рядом, но смотрим в другую сторону
                    is DetectionResult.NearbyNotLooking -> {
                        _uiState.update {
                            it.copy(
                                arrowAngle = azimuth,
                                statusMessage = "Рядом: ${result.sight.name}. Повернитесь к нему!",
                                detectedSight = null
                            )
                        }
                    }
                    // Смотрим прямо на объект
                    is DetectionResult.LookingAt -> {
                        _uiState.update {
                            it.copy(
                                arrowAngle = azimuth,
                                statusMessage = "Вы смотрите на: ${result.sight.name}",
                                detectedSight = result.sight
                            )
                        }
                    }
                }
            }
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
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.refreshFromServer()
            _uiState.update { it.copy(isLoading = false) }

            result
                .onSuccess { _events.emit("Данные обновлены") }
                .onFailure { e ->
                    // Сохраняем имеющийся кэш, показываем ошибку
                    _events.emit("Ошибка обновления: ${e.message}")
                }
        }
    }
}