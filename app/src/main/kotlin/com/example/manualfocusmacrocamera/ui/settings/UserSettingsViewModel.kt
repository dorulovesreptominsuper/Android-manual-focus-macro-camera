package com.example.manualfocusmacrocamera.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datastore.Aspect
import com.example.datastore.ImageQuality
import com.example.manualfocusmacrocamera.data.UserPreferences
import com.example.manualfocusmacrocamera.data.UserPreferencesProtoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesProtoRepository
) : ViewModel() {

    private val initialState = UserPreferences()
    val hasReady: Boolean
        get() = userPreferences.value !== initialState

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userSettingsFlow
        .map { userPreferencesRepository.toUserPreferences(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialState
        )

    fun updateIsPermissionPurposeExplained(isPermissionPurposeExplained: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsPermissionPurposeExplained(
                isPermissionPurposeExplained
            )
        }
    }

    fun updateIsInitialLightOn(isInitialLightOn: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsInitialLightOn(isInitialLightOn)
        }
    }

    fun updateIsSaveGpsLocation(isSaveGpsLocation: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsSaveGpsLocation(isSaveGpsLocation)
        }
    }

    fun updateIsPreviewFullScreen(isPreviewFullScreen: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIsPreviewFullScreen(isPreviewFullScreen)
        }
    }

    fun updateQuality(quality: UserPreferences.Quality) {
        viewModelScope.launch {
            val protoQuality = when (quality) {
                UserPreferences.Quality.HIGH -> ImageQuality.HIGH
                UserPreferences.Quality.MIDDLE -> ImageQuality.MIDDLE
                UserPreferences.Quality.LOW -> ImageQuality.LOW
            }
            userPreferencesRepository.updateQuality(protoQuality)
        }
    }

    fun updateAspect(aspect: UserPreferences.AspectRatio) {
        viewModelScope.launch {
            val protoAspect = when (aspect) {
                UserPreferences.AspectRatio.FOUR_THREE -> Aspect.Four_Three
                UserPreferences.AspectRatio.SIXTEEN_NINE -> Aspect.SIXTEEN_NINE
            }
            userPreferencesRepository.updateAspect(protoAspect)
        }
    }
}
