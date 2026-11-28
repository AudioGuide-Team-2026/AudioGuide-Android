package com.project.audioguide.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.project.audioguide.R
import com.project.audioguide.databinding.ActivityMainBinding
import com.project.audioguide.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private lateinit var narration: SightNarration
    private lateinit var detailsBack: OnBackPressedCallback
    private var descriptionExpanded = false

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

        narration = SightNarration(applicationContext, ::updateActionButtons) { message ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
        setupScreen(savedInstanceState)

        observeViewModel()
        ensureLocationPermissionAndStart()
    }

    private fun setupScreen(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnListen.setOnClickListener {
            val description = viewModel.uiState.value.detectedSight?.description?.trim()
            if (description != null && description.isEmpty()) return@setOnClickListener
            narration.play(description)
        }
        binding.btnPause.setOnClickListener { narration.pause() }
        updateActionButtons()

        detailsBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding.details.isVisible = false
                binding.home.isVisible = true
                isEnabled = false
            }
        }
        onBackPressedDispatcher.addCallback(this, detailsBack)

        binding.btnDetails.setOnClickListener {
            viewModel.uiState.value.detectedSight?.let { sight ->
                openDetails(sight.name, sight.description)
            }
        }
        binding.toggleDescription.setOnClickListener {
            setDescriptionExpanded(!descriptionExpanded)
        }

        val savedName = savedInstanceState?.getString("sight_name")
        val savedDescription = savedInstanceState?.getString("sight_description")
        if (savedName != null && savedDescription != null) {
            openDetails(savedName, savedDescription, savedInstanceState.getBoolean("description_expanded"))
        }
    }

    private fun openDetails(name: String, description: String, expanded: Boolean = false) {
        binding.sightName.text = name
        binding.sightDescription.text = description
        binding.home.isVisible = false
        binding.details.isVisible = true
        detailsBack.isEnabled = true
        binding.details.scrollTo(0, 0)
        setDescriptionExpanded(expanded)
        binding.sightName.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun setDescriptionExpanded(expanded: Boolean) {
        descriptionExpanded = expanded
        binding.sightDescription.maxLines = if (expanded) Int.MAX_VALUE else 7
        binding.toggleDescription.rotation = if (expanded) 180f else 0f
        binding.toggleDescription.contentDescription = getString(
            if (expanded) R.string.guide_collapse_description else R.string.guide_expand_description
        )
        binding.sightDescription.doOnLayout {
            val layout = binding.sightDescription.layout
            val lastLine = (layout?.lineCount ?: 0) - 1
            binding.toggleDescription.isVisible = expanded ||
                (layout != null && lastLine >= 0 && layout.getEllipsisCount(lastLine) > 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (binding.details.isVisible) {
            outState.putString("sight_name", binding.sightName.text.toString())
            outState.putString("sight_description", binding.sightDescription.text.toString())
            outState.putBoolean("description_expanded", descriptionExpanded)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        // Если пользователь выдал разрешение в настройках и вернулся в приложение
        if (hasLocationPermission()) {
            viewModel.startTracking()
        }
    }

    override fun onStop() {
        narration.pause()
        super.onStop()
    }

    override fun onDestroy() {
        narration.close()
        super.onDestroy()
    }

    private fun updateActionButtons() {
        val sight = viewModel.uiState.value.detectedSight
        val canListen = sight?.description?.isNotBlank() ?: narration.canResume
        binding.btnDetails.isEnabled = sight != null
        binding.btnListen.isEnabled = !narration.isPlaying && canListen
        binding.btnPause.isEnabled = narration.isPlaying
    }

    // СЛУШАЕМ ЧТО НАСЧИТАЛ VIEWMODEL И ПОКАЗЫВАЕМ НА ЭКРАНЕ
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        val sight = state.detectedSight
                        val title = sight?.name ?: state.statusMessage
                        if (binding.tvInfo.text != title) binding.tvInfo.text = title
                        updateActionButtons()
                    }
                }
                if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    launch {
                        viewModel.uiState.map { it.arrowAngle }.distinctUntilChanged().collect { angle ->
                            Log.d("AudioGuide", "Азимут: $angle°")
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