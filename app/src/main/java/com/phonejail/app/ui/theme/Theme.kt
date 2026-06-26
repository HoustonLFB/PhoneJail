package com.phonejail.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF7C5CFF)
private val BrandDark = Color(0xFF5B3FE0)
private val Ink = Color(0xFF10141C)
private val Surface = Color(0xFF181D27)
private val Good = Color(0xFF3DDC97)

private val DarkColors = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = Good,
    background = Ink,
    onBackground = Color(0xFFE7E9EE),
    surface = Surface,
    onSurface = Color(0xFFE7E9EE),
    surfaceVariant = Color(0xFF222836),
    onSurfaceVariant = Color(0xFF9AA3B2),
)

private val LightColors = lightColorScheme(
    primary = BrandDark,
    onPrimary = Color.White,
    secondary = Color(0xFF1FAE78),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
)

@Composable
fun PhoneJailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
