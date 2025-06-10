package com.example.manualfocusmacrocamera

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.manualfocusmacrocamera.ui.SettingsBottomSheet
import com.example.manualfocusmacrocamera.ui.camera.CameraScreen
import com.example.manualfocusmacrocamera.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val keyEventState = mutableStateOf(Pair(0, 0L))

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var showSheet by remember { mutableStateOf(false) }

                Scaffold(
                    contentWindowInsets = WindowInsets(0.dp),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "設定")
                        }
                    }
                ) { innerPadding ->
                    CameraScreen(
                        modifier = Modifier.padding(innerPadding),
                        clickedVolumeKey = keyEventState.value,
                        showSnackbar = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        showSnackbarWithCloseButton = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message, "OK")
                            }
                        },
                    )

                    SettingsBottomSheet(
                        showSheet = showSheet,
                        setShowSheet = { showSheet = it }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            keyEventState.value = Pair(keyCode, System.currentTimeMillis())
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
