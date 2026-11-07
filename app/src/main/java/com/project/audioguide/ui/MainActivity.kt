package com.project.audioguide.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.audioguide.data.CompassService
import com.project.audioguide.data.LocationService
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import com.project.audioguide.data.database.DatabaseInitializer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Hilt provides the viewModel
    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    // --- Сервисы ---
    private lateinit var locationService: LocationService
    private lateinit var compassService: CompassService
    private val tag = "MainActivity"

    // Лаунчер для запроса разрешений GPS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            Log.d(tag, "Разрешение на GPS получено")
            // GPS запустится сам в onResume
        } else {
            Log.e(tag, "Разрешение на GPS отклонено")
            Toast.makeText(this, "GPS недоступен без разрешения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val initializer = DatabaseInitializer(applicationContext)
        lifecycleScope.launch {
            initializer.initialize()
        }

        locationService = LocationService(this)
        compassService = CompassService(this)

        // Проверяем разрешения для GPS при старте
        if (!locationService.hasPermission()) {
            requestPermissionLauncher.launch(locationService.requiredPermissions())
        }
    }

    override fun onResume() {
        super.onResume()

        // Включаем компас при возвращении на экран
        compassService.start { azimuth ->
            Log.d(tag, "Компас: %.1f°".format(azimuth))
        }

        // Включаем GPS, если есть разрешение
        if (locationService.hasPermission()) {
            locationService.startLocationUpdates(
                onLocation = { location ->
                    Log.d(tag, "GPS: lat=${location.latitude}, lon=${location.longitude}, acc=${location.accuracy}м")
                },
                onError = { e ->
                    Log.e(tag, "Ошибка GPS: ${e.message}")
                }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Обязательно отключаем компас и GPS в фоне для экономии батареи
        compassService.stop()
        locationService.stopLocationUpdates()
    }
}