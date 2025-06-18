package com.example.manualfocusmacrocamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
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
    shouldShowPermissionExplanationDialog: Boolean,
    onDialogOkClick: () -> Unit,
    onProcessFinish: (hasCameraPermission: Boolean) -> Unit,
) {
    val context = LocalContext.current
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

    val needCameraPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        context.findActivity(),
        Manifest.permission.CAMERA
    )
    val needLocationPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        context.findActivity(),
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val explainDialogTarget =
        if (shouldShowPermissionExplanationDialog || (needCameraPermissionRationale && needLocationPermissionRationale)) {
            ExplainTargetPermission.BOTH
        } else if (needCameraPermissionRationale) {
            ExplainTargetPermission.CAMERA
        } else if (needLocationPermissionRationale) {
            ExplainTargetPermission.LOCATION
        } else null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val hasCameraPermission =
            context.checkPermission(Manifest.permission.CAMERA)
        onProcessFinish(hasCameraPermission)
    }


    explainDialogTarget?.let {
        ExplainPermissionsPurposeDialog(
            dialogTitle = "アプリの権限について",
            onOkClick = {
                onDialogOkClick()
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            targetPermission = it
        )
    } ?: run {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}