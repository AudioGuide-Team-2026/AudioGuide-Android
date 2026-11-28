package com.project.audioguide.ui

import android.graphics.Rect
import android.os.SystemClock
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.project.audioguide.FakeCompassService
import com.project.audioguide.FakeLocationService
import com.project.audioguide.FakeSightRepository
import com.project.audioguide.R
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Чтобы на эмуляторе не появлялся системный диалог запроса GPS
    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Inject lateinit var repository: FakeSightRepository
    @Inject lateinit var location: FakeLocationService
    @Inject lateinit var compass: FakeCompassService

    private val itmoMain = Sight(
        id = 1,
        address = "Кронверкский пр., 49",
        location = Location(59.956363, 30.310011),
        name = "Главный корпус ИТМО",
        description = "Описание главного корпуса"
    )

    // 20 метров южнее объекта: азимут на объект примерно 0 градусов
    private val nearUser = Location(59.956183, 30.310011)
    // Примерно 3.6 км от объекта
    private val farUser = Location(59.927288, 30.338353)

    @Before
    fun setUp() {
        hiltRule.inject()
        repository.sights.value = listOf(itmoMain)
    }

    private fun launchScreen() = ActivityScenario.launch(MainActivity::class.java)

    private fun assertInfo(text: String) {
        onView(withId(R.id.tvInfo)).check(matches(withText(containsString(text))))
    }

    @Test
    fun screen_showsMainElements() {
        launchScreen().use {
            onView(withId(R.id.btnListen)).check(matches(isDisplayed()))
            onView(withId(R.id.btnPause)).check(matches(isDisplayed()))
            onView(withId(R.id.tvInfo)).check(matches(isDisplayed()))
            onView(withId(R.id.btnDetails)).check(matches(isDisplayed()))
            onView(withId(R.id.btnReloadData)).check(matches(isDisplayed()))
            onView(withId(R.id.btnReloadData)).check(matches(withText(R.string.guide_reload_data)))
            onView(withId(R.id.btnReloadData)).check(matches(isEnabled()))
            onView(withId(R.id.btnDetails)).check(matches(withText(R.string.guide_details)))
            onView(withId(R.id.btnDetails)).check(matches(not(isEnabled())))
            onView(withId(R.id.btnListen)).check(matches(not(isEnabled())))
            onView(withId(R.id.btnPause)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun reloadButton_click_requestsDataFromServer() {
        launchScreen().use {
            onView(withId(R.id.btnReloadData)).perform(click())

            assertEquals(1, repository.refreshCalls)
        }
    }

    @Test
    fun reloadButton_isDisabledWhileLoading() {
        val gate = CompletableDeferred<Unit>()
        repository.refreshGate = gate

        launchScreen().use {
            onView(withId(R.id.btnReloadData)).perform(click())
            onView(withId(R.id.btnReloadData)).check(matches(not(isEnabled())))

            gate.complete(Unit)

            onView(withId(R.id.btnReloadData)).check(matches(isEnabled()))
        }
    }

    @Test
    fun lookingAtSight_showsNameAndDescription() {
        location.location.value = nearUser
        compass.azimuth.value = 355f

        launchScreen().use {
            assertInfo("Главный корпус ИТМО")
            onView(withId(R.id.btnListen)).check(matches(isEnabled()))
            onView(withId(R.id.btnPause)).check(matches(not(isEnabled())))
            onView(withId(R.id.btnDetails)).perform(click())
            assertEquals(0, repository.refreshCalls)
            onView(withId(R.id.sightName)).check(matches(withText(itmoMain.name)))
            onView(withId(R.id.sightDescription)).check(matches(withText(itmoMain.description)))

            compass.azimuth.value = 120f
            onView(withId(R.id.sightName)).check(matches(withText(itmoMain.name)))
            pressBack()
            assertInfo("Повернитесь к нему")
        }
    }

    @Test
    fun longDescription_scrollsToLastLineAfterExpansion() {
        val description = "Длинная строка описания.\n".repeat(65) + "Последняя строка."
        repository.sights.value = listOf(itmoMain.copy(description = description))
        location.location.value = nearUser
        compass.azimuth.value = 355f

        launchScreen().use { scenario ->
            onView(withId(R.id.btnDetails)).perform(click())
            onView(withId(R.id.toggleDescription)).perform(click())

            onView(withId(R.id.details)).check(matches(isCompletelyDisplayed()))
            onView(withId(R.id.toggleDescription)).check(matches(not(isDisplayed())))

            repeat(6) {
                onView(withId(R.id.details)).perform(swipeUp())
            }

            onView(withId(R.id.toggleDescription)).check(matches(isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.guide_collapse_description)))
            scenario.onActivity { activity ->
                val descriptionView = activity.findViewById<TextView>(R.id.sightDescription)
                val layout = checkNotNull(descriptionView.layout)
                val lastLine = layout.lineCount - 1
                val visible = Rect()
                assertEquals(description.length, layout.getLineEnd(lastLine))
                assertEquals(0, layout.getEllipsisCount(lastLine))
                assertTrue(descriptionView.getLocalVisibleRect(visible))
                val lastLineTop = descriptionView.totalPaddingTop + layout.getLineTop(lastLine)
                val lastLineBottom = descriptionView.totalPaddingTop + layout.getLineBottom(lastLine)
                assertTrue(visible.top <= lastLineTop && visible.bottom >= lastLineBottom)
            }
        }
    }

    @Test
    fun nearButNotLooking_asksToTurn() {
        location.location.value = nearUser
        compass.azimuth.value = 120f

        launchScreen().use {
            assertInfo("Повернитесь к нему")
            onView(withId(R.id.tvInfo)).check(matches(withText(not(containsString("Описание")))))
        }
    }

    @Test
    fun farAway_showsNothingNearby() {
        location.location.value = farUser

        launchScreen().use {
            assertInfo("В радиусе 50 метров ничего нет.")
        }
    }

    @Test
    fun emptyDatabase_showsEmptyBaseMessage() {
        repository.sights.value = emptyList()
        location.location.value = nearUser

        launchScreen().use {
            assertInfo("База объектов пуста")
        }
    }

    @Test
    fun turningToSight_updatesScreen() {
        location.location.value = nearUser
        compass.azimuth.value = 120f

        launchScreen().use {
            assertInfo("Повернитесь к нему")

            compass.azimuth.value = 0f

            assertInfo("Главный корпус ИТМО")
        }
    }

    // Нефункциональное требование: информация об объекте или его отсутствии
    // должна отобразиться не позже чем через 3 секунды

    private val maxDisplayTimeMs = 3000L

    /** Ждет, пока на экране появится текст, и возвращает, сколько миллисекунд это заняло. */
    private fun waitForInfo(text: String, timeoutMs: Long): Long {
        val start = SystemClock.elapsedRealtime()
        while (true) {
            try {
                assertInfo(text)
                return SystemClock.elapsedRealtime() - start
            } catch (e: Throwable) {
                if (SystemClock.elapsedRealtime() - start > timeoutMs) throw e
                Thread.sleep(50)
            }
        }
    }

    @Test
    fun sightInfo_isShownWithinThreeSeconds() {
        location.location.value = nearUser
        compass.azimuth.value = 120f

        launchScreen().use {
            assertInfo("Повернитесь к нему")

            // "Наводим" устройство на объект
            compass.azimuth.value = 0f
            val elapsed = waitForInfo("Главный корпус ИТМО", maxDisplayTimeMs)

            assertTrue("Информация появилась за $elapsed мс, ожидалось меньше 3000", elapsed < maxDisplayTimeMs)
        }
    }

    @Test
    fun noSightInfo_isShownWithinThreeSeconds() {
        location.location.value = nearUser
        compass.azimuth.value = 0f

        launchScreen().use {
            assertInfo("Главный корпус ИТМО")

            // Уходим далеко от объекта
            location.location.value = farUser
            val elapsed = waitForInfo("В радиусе 50 метров ничего нет.", maxDisplayTimeMs)

            assertTrue("Сообщение появилось за $elapsed мс, ожидалось меньше 3000", elapsed < maxDisplayTimeMs)
        }
    }
}
