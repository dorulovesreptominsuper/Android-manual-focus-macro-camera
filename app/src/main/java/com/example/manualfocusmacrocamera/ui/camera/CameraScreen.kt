package com.example.manualfocusmacrocamera.ui.camera

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var maxFocusDistance by remember { mutableFloatStateOf(0f) }
    var focusDistance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        maxFocusDistance = viewModel.setupCamera(previewView, lifecycleOwner)

    }

    Column(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.weight(1f)
        )

        Text(
            "この端末の最短フォーカス距離: ${String.format("%.1f", 100 / viewModel.diopters)} cm",
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "遠")
            Slider(
                value = focusDistance,
                onValueChange = {
                    focusDistance = it
                    viewModel.setManualFocus(it)
                },
                valueRange = 0f..maxFocusDistance,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            Text(text = "近")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = viewModel::switchTorch,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text("ライト切替")
            }

            Button(
                onClick = {
                    viewModel.takePhoto(
                        context,
                        onSaved = { uri ->
                            Toast.makeText(context, "保存成功: $uri", Toast.LENGTH_SHORT)
                                .show()
                        },
                        onError = { error ->
                            Toast.makeText(
                                context,
                                "保存失敗: ${error.message}",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    )
                },
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text("📸 撮影")
            }
        }
    }
}