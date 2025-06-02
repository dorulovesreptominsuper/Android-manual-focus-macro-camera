package com.example.manualfocusmacrocamera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.manualfocusmacrocamera.ui.camera.CameraViewModel
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: CameraViewModel = hiltViewModel(),
    showSheet: Boolean,
    setShowSheet: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var wifiEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(true) }
    val cameraDiopters = viewModel.diopters

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { setShowSheet(false) },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                val focusDistanceText = String.format(Locale.US, "%.2f", 100 / cameraDiopters.value)
                Text(
                    buildAnnotatedString {
                        append(" このスマホの最短フォーカス距離は ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                        append(focusDistanceText)
                        append("cm")
                        pop() // 太字解除
                        append(" だよ。")
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("アプリ起動時ライトを自動でONにする", modifier = Modifier.weight(1f))
                    AnimatedColorSwitch(
                        checked = wifiEnabled,
                        onCheckedChange = { wifiEnabled = it }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("画像にGPS情報を付与する", modifier = Modifier.weight(1f))
                    AnimatedColorSwitch(
                        checked = bluetoothEnabled,
                        onCheckedChange = { bluetoothEnabled = it }
                    )
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}
