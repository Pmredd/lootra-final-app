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

// Premium Dark ColorScheme for Lootra
private val DarkColorScheme = darkColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = PremiumSurfaceCard,
    onPrimaryContainer = TextLight,
    secondary = RoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = PremiumSurface,
    onSecondaryContainer = TextLight,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = PremiumBlack,
    onBackground = TextLight,
    surface = PremiumSurface,
    onSurface = TextLight,
    surfaceVariant = PremiumSurfaceCard,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF2E2445)
)

// Clean Light ColorScheme for Lootra
private val LightColorScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5F3FF),
    onPrimaryContainer = DeepPurple,
    secondary = RoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = RoyalBlue,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = Color(0xFFFAF9FD),
    onBackground = Color(0xFF0D0B14),
    surface = Color.White,
    onSurface = Color(0xFF0D0B14),
    surfaceVariant = Color(0xFFF1EEF9),
    onSurfaceVariant = Color(0xFF6B6580),
    outline = Color(0xFFE2DDF0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is premium, but for a strict custom brand visual we prefer our highly tailored scheme
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
