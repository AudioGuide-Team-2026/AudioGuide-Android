package com.project.audioguide.ui

import com.project.audioguide.domain.SightDetector
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import com.project.audioguide.fakes.FakeCompassService
import com.project.audioguide.fakes.FakeLocationService
import com.project.audioguide.fakes.FakeSightRepository
import com.project.audioguide.ui.main.MainViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val itmoMain = Sight(
        id = 1,
        address = "Кронверкский пр., 49",
        location = Location(59.956363, 30.310011),
        name = "Главный корпус ИТМО",
        description = "Описание"
    )

    // 20 метров южнее объекта: азимут на объект примерно 0 градусов
    private val nearUser = Location(59.956183, 30.310011)
    // Примерно 3.6 км от объекта
    private val farUser = Location(59.927288, 30.338353)

    private lateinit var repository: FakeSightRepository
    private lateinit var location: FakeLocationService
    private lateinit var compass: FakeCompassService

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeSightRepository(listOf(itmoMain))
        location = FakeLocationService()
        compass = FakeCompassService()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        MainViewModel(location, compass, repository, SightDetector())

    @Test
    fun init_loadsInitialData() {
        createViewModel()

        assertEquals(1, repository.loadInitialDataCalls)
    }

    @Test
    fun startTracking_lookingAtSight_showsSight() {
        location.location.value = nearUser
        compass.azimuth.value = 355f
        val vm = createViewModel()

        vm.startTracking()

        val state = vm.uiState.value
        assertEquals(itmoMain, state.detectedSight)
        assertEquals("Вы смотрите на: Главный корпус ИТМО", state.statusMessage)
        assertEquals(355f, state.arrowAngle, 0.01f)
    }

    @Test
    fun startTracking_nearButNotLooking_asksToTurn() {
        location.location.value = nearUser
        compass.azimuth.value = 120f
        val vm = createViewModel()

        vm.startTracking()

        val state = vm.uiState.value
        assertNull(state.detectedSight)
        assertEquals("Рядом: Главный корпус ИТМО. Повернитесь к нему!", state.statusMessage)
    }

    @Test
    fun startTracking_farAway_reportsNothingNearby() {
        location.location.value = farUser
        val vm = createViewModel()

        vm.startTracking()

        val state = vm.uiState.value
        assertNull(state.detectedSight)
        assertEquals("В радиусе 50 метров ничего нет.", state.statusMessage)
    }

    @Test
    fun startTracking_emptyDatabase_reportsEmptyBase() {
        repository.sights.value = emptyList()
        location.location.value = nearUser
        val vm = createViewModel()

        vm.startTracking()

        val state = vm.uiState.value
        assertNull(state.detectedSight)
        assertEquals("База объектов пуста", state.statusMessage)
    }

    @Test
    fun startTracking_reactsToCompassChange() {
        location.location.value = nearUser
        compass.azimuth.value = 120f
        val vm = createViewModel()
        vm.startTracking()
        assertNull(vm.uiState.value.detectedSight)

        compass.azimuth.value = 0f

        assertEquals(itmoMain, vm.uiState.value.detectedSight)
    }

    @Test
    fun startTracking_reactsToDatabaseUpdate() {
        repository.sights.value = emptyList()
        location.location.value = nearUser
        compass.azimuth.value = 0f
        val vm = createViewModel()
        vm.startTracking()
        assertEquals("База объектов пуста", vm.uiState.value.statusMessage)

        repository.sights.value = listOf(itmoMain)

        assertEquals(itmoMain, vm.uiState.value.detectedSight)
    }

    @Test
    fun startTracking_calledTwice_doesNotBreakState() {
        location.location.value = nearUser
        compass.azimuth.value = 0f
        val vm = createViewModel()

        vm.startTracking()
        vm.startTracking()

        assertEquals(itmoMain, vm.uiState.value.detectedSight)
    }

    @Test
    fun reloadSightsFromServer_success_emitsEventAndStopsLoading() = runTest {
        val vm = createViewModel()
        val events = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.reloadSightsFromServer()

        assertEquals(listOf("Данные обновлены"), events)
        assertFalse(vm.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun reloadSightsFromServer_failure_emitsErrorAndStopsLoading() = runTest {
        repository.refreshResult = Result.failure(RuntimeException("Нет сети"))
        val vm = createViewModel()
        val events = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.reloadSightsFromServer()

        assertEquals(listOf("Ошибка обновления: Нет сети"), events)
        assertFalse(vm.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun reloadSightsFromServer_showsLoadingWhileRequestIsRunning() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.refreshGate = gate
        val vm = createViewModel()

        vm.reloadSightsFromServer()
        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        assertFalse(vm.uiState.value.isLoading)
    }
}
