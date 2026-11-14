package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VanGoghStarGold,
    onPrimary = Color(0xFF2A1A00),
    primaryContainer = Color(0xFF4A3400),
    onPrimaryContainer = VanGoghSolarYellow,
    secondary = CyberNeonCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9CF4FF),
    tertiary = GaudiCoralRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5E1318),
    onTertiaryContainer = Color(0xFFFFDADA),
    background = CyberMidnight,
    onBackground = TextPrimaryLight,
    surface = CyberSurfaceDark,
    onSurface = TextPrimaryLight,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineBorder,
    outlineVariant = Color(0xFF2D274A)
)

private val LightColorScheme = darkColorScheme(
    primary = VanGoghStarGold,
    secondary = CyberNeonCyan,
    tertiary = GaudiMosaicOrange,
    background = CyberMidnight,
    surface = CyberSurfaceDark,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

@Composable
fun CyberComicsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Comic readers benefit immensely from high-contrast dark theme by default
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
