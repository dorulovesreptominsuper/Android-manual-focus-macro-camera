package com.example.manualfocusmacrocamera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manualfocusmacrocamera.data.UserPreferences
import com.example.manualfocusmacrocamera.ui.camera.MacroCameraInfo
import com.example.manualfocusmacrocamera.ui.settings.UserSettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settingsViewModel: UserSettingsViewModel = hiltViewModel(),
    macroCameraInfo: MacroCameraInfo,
    showSheet: Boolean,
    setShowSheet: (Boolean) -> Unit,
    onImageCaptureSettingChenged: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val minimumFocusDistance = macroCameraInfo.minimumFocusDistance

    val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val settings by remember(userPreferences) { mutableStateOf(userPreferences.first) }

    if (!userPreferences.second) return

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { setShowSheet(false) },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val focusDistanceText =
                    String.format(Locale.US, "%.2f", 100 / minimumFocusDistance)
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
                        checked = settings.isInitialLightOn,
                        onCheckedChange = { settingsViewModel.updateIsInitialLightOn(it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("画像にGPS情報を付与する", modifier = Modifier.weight(1f))
                    AnimatedColorSwitch(
                        checked = settings.isSaveGpsLocation,
                        onCheckedChange = { settingsViewModel.updateIsSaveGpsLocation(it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("プレビューをフルスクリーンにする", modifier = Modifier.weight(1f))
                    AnimatedColorSwitch(
                        checked = settings.isPreviewFullScreen,
                        onCheckedChange = { settingsViewModel.updateIsPreviewFullScreen(it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("撮影写真のアスペクト比")
                    Spacer(modifier = Modifier.weight(1f))
                    M3ExpressiveButtonGroup(
                        modifier = Modifier.wrapContentWidth(),
                        options = UserPreferences.AspectRatio.entries.toList()
                            .map { it.toDisplayString() },
                        currentSelectedIndex = settings.aspect.ordinal,
                        onOptionSelected = {
                            val targetAspect = UserPreferences.AspectRatio.entries[it]
                            settingsViewModel.updateAspect(
                                aspect = targetAspect,
                                onUpdateCompleted = onImageCaptureSettingChenged
                            )
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    itemVerticalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("写真の画質")
                    Spacer(modifier = Modifier.weight(1f))
                    M3ExpressiveButtonGroup(
                        modifier = Modifier.wrapContentWidth(),
                        options = UserPreferences.Quality.entries.toList()
                            .map { it.toDisplayString() },
                        currentSelectedIndex = settings.quality.ordinal,
                        onOptionSelected = {
                            val targetAspect = UserPreferences.Quality.entries[it]
                            settingsViewModel.updateQuality(
                                quality = targetAspect,
                                onUpdateCompleted = onImageCaptureSettingChenged
                            )
                        }
                    )
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

private fun UserPreferences.AspectRatio.toDisplayString(): String {
    return when (this) {
        UserPreferences.AspectRatio.FOUR_THREE -> "4:3"
        UserPreferences.AspectRatio.SIXTEEN_NINE -> "16:9"
    }
}

private fun UserPreferences.Quality.toDisplayString(): String {
    return when (this) {
        UserPreferences.Quality.HIGH -> "最高"
        UserPreferences.Quality.MIDDLE -> "中"
        UserPreferences.Quality.LOW -> "低"
    }
}
