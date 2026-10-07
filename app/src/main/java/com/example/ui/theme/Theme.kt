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
    primary = DarkGreenLight,
    onPrimary = Color.White,
    primaryContainer = DarkGreenDark,
    onPrimaryContainer = DarkGreenContainer,
    secondary = ForestGreenLight,
    onSecondary = Color.White,
    secondaryContainer = DarkGreenDark,
    onSecondaryContainer = ForestGreenContainer,
    tertiary = Color(0xFF20C997),
    onTertiary = Color.Black,
    tertiaryContainer = DarkGreenDark,
    onTertiaryContainer = DarkGreenContainer,
    background = Color(0xFF0D1811),
    surface = Color(0xFF132219),
    onBackground = Color(0xFFF1F8F3),
    onSurface = Color(0xFFF1F8F3),
    surfaceVariant = Color(0xFF1B2E22),
    onSurfaceVariant = Color(0xFFC0D2C4),
    outline = Color(0xFF2D4736)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = DarkGreenOnContainer,
    secondary = ForestGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = ForestGreenContainer,
    onSecondaryContainer = DarkGreenDark,
    tertiary = DarkGreenLight,
    onTertiary = Color.White,
    tertiaryContainer = DarkGreenContainer,
    onTertiaryContainer = DarkGreenDark,
    background = WhiteBackground,
    onBackground = CharcoalDarkText,
    surface = PureWhiteSurface,
    onSurface = CharcoalDarkText,
    surfaceVariant = WhiteSurfaceVariant,
    onSurfaceVariant = MutedDarkText,
    outline = OutlineGreen
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve high-contrast Nigerian Dark Green & White identity
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
