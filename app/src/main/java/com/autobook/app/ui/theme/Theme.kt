package com.autobook.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ──── 品牌色 ────
val Green500 = Color(0xFF4CAF50)
val Green700 = Color(0xFF388E3C)
val Green200 = Color(0xFFA5D6A7)
val Red500 = Color(0xFFF44336)
val Orange500 = Color(0xFFFF9800)
val Blue500 = Color(0xFF2196F3)
val Purple500 = Color(0xFF9C27B0)
val Grey50 = Color(0xFFFAFAFA)
val Grey100 = Color(0xFFF5F5F5)
val Grey200 = Color(0xFFEEEEEE)
val Grey800 = Color(0xFF424242)
val Grey900 = Color(0xFF212121)

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green200,
    secondary = Blue500,
    onSecondary = Color.White,
    background = Grey50,
    surface = Color.White,
    onBackground = Grey900,
    onSurface = Grey900,
    error = Red500,
    surfaceVariant = Grey100
)

private val DarkColorScheme = darkColorScheme(
    primary = Green200,
    onPrimary = Grey900,
    primaryContainer = Green700,
    secondary = Blue500,
    onSecondary = Color.White,
    background = Grey900,
    surface = Grey800,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFEF9A9A),
    surfaceVariant = Color(0xFF333333)
)

private val AppTypography = androidx.compose.material3.Typography()

@Composable
fun AutoBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
