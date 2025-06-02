package com.example.manualfocusmacrocamera.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.ui.theme.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val AquaBlue = Color(0xFF23C6E8) // 主役の水色
val Turquoise = Color(0xFF23DAC6)
val AquaBlueLight = Color(0xFF7EEFFF)
val OnAquaBlue = Color.White

// 補色・アクセント
val Secondary = Color(0xFF26A69A)
val OnSecondary = Color.White
val Tertiary = Color(0xFFF4D35E)
val OnTertiary = Color(0xFF3E2723)
val Error = Color(0xFFE57373)
val OnError = Color.White

// サーフェス・背景
val Background = Color(0xFFE6FBFF)
val OnBackground = Color(0xFF123943)
val Surface = Color(0xFFD3F7FC)
val OnSurface = Color(0xFF134651)
val SurfaceVariant = Color(0xFFB0EAF4)
val OnSurfaceVariant = Color(0xFF106D83)
val Outline = Color(0xFF62D9ED)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = AquaBlue,
    onPrimary = OnAquaBlue,
    primaryContainer = AquaBlueLight,
    onPrimaryContainer = Color(0xFF00363D),
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF003731),
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = Color(0xFFFFF9E6),
    onTertiaryContainer = Color(0xFF493600),
    error = Error,
    onError = OnError,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Turquoise,
    onPrimary = Color.White,
    primaryContainer = AquaBlue,
    onPrimaryContainer = Color(0xFF00363D),
    secondary = Color(0xFF5DDDD3),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E3836),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFE082),
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF443D33),
    onTertiaryContainer = Color(0xFFFFF9E6),
    error = Color(0xFFEF9A9A),
    onError = Color.Black,
    background = Color(0xFF10272A),
    onBackground = Color(0xFFD4FAFF),
    surface = Color(0xFF15343A),
    onSurface = Color(0xFFC4F3FF),
    surfaceVariant = Color(0xFF227F91),
    onSurfaceVariant = Color(0xFFB0EAF4),
    outline = Color(0xFF78E6FB),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}