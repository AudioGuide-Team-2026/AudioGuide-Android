package com.project.audioguide.ui

import android.Manifest
import android.util.Log
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.project.audioguide.data.CompassService
import com.project.audioguide.data.LocationService
import androidx.lifecycle.repeatOnLifecycle
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import com.project.audioguide.data.database.DatabaseInitializer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

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

        if (!granted) {
            Toast.makeText(this, "GPS недоступен без разрешения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // изначальная загрузка данных из файла sights.kt
        lifecycleScope.launch {
            viewModel.loadInitialData()
        }

        locationService = LocationService(this)
        compassService = CompassService(this)

        // Проверяем разрешения для GPS при старте
        if (!locationService.hasPermission()) {
            requestPermissionLauncher.launch(locationService.requiredPermissions())
        }
        // при нажатии на кнопку в бд загружаются данные с сервера
        binding.btnReloadData.setOnClickListener {
            viewModel.reloadSightsFromServer()
            Toast.makeText(this, "Данные обновлены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Включаем компас при возвращении на экран
        compassService.start { azimuth ->
            Log.d(tag, "Компас: %.1f°".format(azimuth))
        // Запрашиваем разрешения, если их нет
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        // СЛУШАЕМ ЧТО НАСЧИТАЛ VIEWMODEL И ПОКАЗЫВАЕМ НА ЭКРАНЕ
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Выводим статус (Поиск... / Ничего нет / Рядом есть...)
                    binding.tvInfo.text = state.statusMessage
                    
                    // Если нашли объект, добавляем его описание
                    state.detectedSight?.let { sight ->
                        binding.tvInfo.append("\n\n${sight.description}")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}