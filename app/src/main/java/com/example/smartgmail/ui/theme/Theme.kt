package com.example.smartgmail.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrillDarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    secondary = BrandPurple,
    tertiary = BrandRed,
    background = DeepSpace,
    surface = DeepSpace,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = OnSurfaceSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0x1AFFFFFF)
)

@Composable
fun SmartGmailTheme(
    darkTheme: Boolean = true, // Force dark theme for the Brill aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BrillDarkColorScheme,
        typography = Typography,
        content = content
    )
}
