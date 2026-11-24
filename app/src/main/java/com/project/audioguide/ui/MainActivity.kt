package com.project.audioguide.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.project.audioguide.data.CompassService
import com.project.audioguide.data.LocationService
import com.project.audioguide.data.database.DatabaseInitializer
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    // --- Сервисы ---
    private lateinit var locationService: LocationService
    private lateinit var compassService: CompassService
    private val tag = "MainActivity"

    @Inject
    lateinit var initializer: DatabaseInitializer

    // Лаунчер для запроса разрешений GPS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            viewModel.startTracking()
        } else {
            Toast.makeText(this, "GPS недоступен без разрешения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // изначальная загрузка данных из файла sights.kt
        viewModel.loadInitialData()

        locationService = LocationService(this)
        compassService = CompassService(this)

        // Проверяем разрешения для GPS при старте
        if (locationService.hasPermission()) {
            viewModel.startTracking()
        } else {
            requestPermissionLauncher.launch(locationService.requiredPermissions())
        }

        // при нажатии на кнопку в бд загружаются данные с сервера
        binding.btnReloadData.setOnClickListener {
            viewModel.reloadSightsFromServer()
        }

        // СЛУШАЕМ ЧТО НАСЧИТАЛ VIEWMODEL И ПОКАЗЫВАЕМ НА ЭКРАНЕ
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnReloadData.isEnabled = !state.isLoading

                    // Выводим статус (Поиск... / Ничего нет / Рядом есть...)
                    binding.tvInfo.text = state.statusMessage

                    // Если нашли объект, добавляем его описание
                    state.detectedSight?.let { sight ->
                        binding.tvInfo.append("\n\n${sight.description}")
                    }

                    // Показываем результат загрузки с сервера, когда он пришёл
                    state.message?.let { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Включаем компас при возвращении на экран
        compassService.start { azimuth ->
            Log.d(tag, "Компас: %.1f°".format(azimuth))
        }
    }

    override fun onPause() {
        super.onPause()
        // Останавливаем компас при уходе с экрана
        compassService.stop()
    }
}