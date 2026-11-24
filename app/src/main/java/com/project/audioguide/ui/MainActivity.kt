package com.project.audioguide.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

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

        // при нажатии на кнопку в бд загружаются данные с сервера
        binding.btnReloadData.setOnClickListener {
            viewModel.reloadSightsFromServer()
        }

        observeViewModel()
        ensureLocationPermissionAndStart()
    }

    override fun onStart() {
        super.onStart()
        // Если пользователь выдал разрешение в настройках и вернулся в приложение
        if (hasLocationPermission()) {
            viewModel.startTracking()
        }
    }

    // СЛУШАЕМ ЧТО НАСЧИТАЛ VIEWMODEL И ПОКАЗЫВАЕМ НА ЭКРАНЕ
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.btnReloadData.isEnabled = !state.isLoading
                        binding.tvHello.text = "Азимут: %.1f°".format(state.arrowAngle)

                        // Выводим статус (Поиск... / Ничего нет / Рядом есть...)
                        binding.tvInfo.text = state.statusMessage

                        // Если нашли объект, добавляем его описание
                        state.detectedSight?.let { sight ->
                            binding.tvInfo.append("\n\n${sight.description}")
                        }
                    }
                }
                launch {
                    viewModel.events.collect { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Проверяем разрешения для GPS при старте
    private fun ensureLocationPermissionAndStart() {
        if (hasLocationPermission()) {
            viewModel.startTracking()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}