package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun RequestPermissions(
    targetPermissions: List<String>,
    onPermissionRequestFinished: (hasCameraPermission: Boolean) -> Unit,
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val hasCameraPermission =
            context.checkPermission(Manifest.permission.CAMERA)
        onPermissionRequestFinished(hasCameraPermission)
    }

    LaunchedEffect(Unit) {
        if (targetPermissions.isNotEmpty()) {
            permissionLauncher.launch(targetPermissions.toTypedArray())
        }
    }
}

@Composable
fun CheckPermissions(
    onProcessFinish: (List<String>) -> Unit,
) {
    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).filter {
        ContextCompat.checkSelfPermission(
            LocalContext.current,
            it
        ) != PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        onProcessFinish(permissionsToRequest)
    }

    LoadingScreen()
}