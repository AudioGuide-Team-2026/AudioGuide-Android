package com.project.audioguide.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.audioguide.data.database.sights // Импорт файла с достопримечательностями
import com.project.audioguide.data.repository.SightRepository // Импорт репозитория
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    // TODO: Hilt use cases
    private val repository: SightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // TEST
    fun updateCompassAngle(newAngle: Float) {
        _uiState.value = _uiState.value.copy(arrowAngle = newAngle)
    }

    fun reloadSightsFromSource() {
        viewModelScope.launch {
            repository.replaceAll(sights)          // Запись в базу данных
        }
    }
}