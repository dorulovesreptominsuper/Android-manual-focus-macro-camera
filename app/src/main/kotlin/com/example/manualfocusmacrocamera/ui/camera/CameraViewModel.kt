package com.example.manualfocusmacrocamera.ui.camera

import androidx.lifecycle.ViewModel
import com.example.manualfocusmacrocamera.ui.CameraUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.DataStoreFetching)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun updateUiState(newState: CameraUiState) {
        _uiState.value = newState
    }
}