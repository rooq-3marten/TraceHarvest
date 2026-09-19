package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HarvestGreenLight,
    onPrimary = Color.White,
    primaryContainer = HarvestGreenDark,
    onPrimaryContainer = HarvestGreenContainer,
    secondary = SesameAmberLight,
    onSecondary = Color.White,
    secondaryContainer = SesameAmberSecondary,
    onSecondaryContainer = Color.White,
    tertiary = EarthTerracotta,
    background = Color(0xFF121512),
    surface = Color(0xFF1B1E1B),
    onBackground = Color(0xFFE2E4DE),
    onSurface = Color(0xFFE2E4DE),
    surfaceVariant = Color(0xFF262C26)
)

private val LightColorScheme = lightColorScheme(
    primary = HarvestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = HarvestGreenContainer,
    onPrimaryContainer = HarvestGreenDark,
    secondary = SesameAmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = SesameAmberContainer,
    onSecondaryContainer = SesameAmberSecondary,
    tertiary = EarthTerracotta,
    background = SurfaceWarm,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantWarm,
    outline = OutlineBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand palette by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
