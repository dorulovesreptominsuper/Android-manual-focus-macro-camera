package com.example.manualfocusmacrocamera.ui

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import com.example.manualfocusmacrocamera.ui.camera.MacroCameraInfo

sealed interface CameraUiState {
    object DataStoreFetching : CameraUiState
    object CheckPermission : CameraUiState
    data class PermissionRequesting(val targetPermissions: List<String>) : CameraUiState
    object NoCameraPermission : CameraUiState
    object SetupCamera : CameraUiState
    data class CameraOpened(
        val previewView: PreviewView,
        val camera: Camera,
        val capture: ImageCapture,
        val cameraInfo: MacroCameraInfo
    ) : CameraUiState

}