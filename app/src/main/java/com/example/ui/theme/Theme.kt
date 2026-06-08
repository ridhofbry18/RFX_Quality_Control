package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RfxRedAccent,
    onPrimary = Color.White,
    primaryContainer = RfxRedAccentDark,
    onPrimaryContainer = Color.White,
    secondary = Zinc300,
    onSecondary = Color.Black,
    background = CoreDarkBackground,
    onBackground = Color.White,
    surface = CoreDarkSurface,
    onSurface = Color.White,
    outline = CoreDarkBorder,
    surfaceVariant = CoreDarkSurfaceElevated,
    onSurfaceVariant = Zinc300
)

private val LightColorScheme = lightColorScheme(
    primary = RfxRedAccent,
    onPrimary = Color.White,
    primaryContainer = RfxRedAccentLight,
    onPrimaryContainer = Color.White,
    secondary = Zinc600,
    onSecondary = Color.White,
    background = CoreLightBackground,
    onBackground = Color.Black,
    surface = CoreLightSurface,
    onSurface = Color.Black,
    outline = CoreLightBorder,
    surfaceVariant = CoreLightSurfaceElevated,
    onSurfaceVariant = Zinc600
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
