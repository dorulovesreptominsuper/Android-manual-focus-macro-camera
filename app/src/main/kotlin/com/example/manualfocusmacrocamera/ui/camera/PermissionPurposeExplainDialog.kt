@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.manualfocusmacrocamera.ui.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

enum class TargetPermission(val description: String) {
    CAMERA("写真の撮影のためにカメラの権限は必須です。"),
    LOCATION("写真にGPS情報（緯度経度・高度）を記録するためには位置情報権限が必要なので記録したい場合は権限を許可してください。"),
    BOTH("この後にシステム経由でカメラと位置情報の使用権限を求めます。\n撮影のためにカメラの権限は必須です。位置情報権限は必須ではありませんが、写真にGPS情報を付与する場合は必要です。"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExplainPermissionsPurposeDialog(
    dialogTitle: String,
    onOkClick: () -> Unit,
    targetPermission: TargetPermission = TargetPermission.BOTH,
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        BasicAlertDialog(
            onDismissRequest = {},
            modifier = Modifier.background(
                color = Color.White,
                shape = MaterialTheme.shapes.medium
            ),
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                decorFitsSystemWindows = false,
            ),
            content = {
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif,
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dialogTitle,
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = targetPermission.description,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showDialog = false
                                onOkClick()
                            },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(text = "OK", color = Color.White)
                        }
                    }
                }
            }
        )
    }
}

fun Context.findActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> throw IllegalStateException("Context is not an Activity.")
    }
}