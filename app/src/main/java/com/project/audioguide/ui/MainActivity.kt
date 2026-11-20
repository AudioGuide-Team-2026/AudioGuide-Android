package com.project.audioguide.ui

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
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

        // Инициализация базы данных
        lifecycleScope.launch {
            initializer.initialize()
        }

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