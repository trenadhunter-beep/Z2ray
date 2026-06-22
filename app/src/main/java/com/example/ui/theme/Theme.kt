package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.EnglishStrings
import com.example.ui.PersianStrings
import com.example.ui.RussianStrings
import com.example.ui.LocalAppStrings

@Composable
fun MyApplicationTheme(
    themeName: String = "Bento Dark",
    language: String = "en",
    content: @Composable () -> Unit
) {
    // Resolve AppColors
    val resolvedColors = when (themeName) {
        "Cyberpunk Neon" -> CyberpunkNeonTheme
        "Emerald Secure" -> EmeraldSecureTheme
        "Midnight Black" -> MidnightBlackTheme
        else -> BentoDarkTheme
    }

    // Resolve AppStrings
    val resolvedStrings = when (language) {
        "fa" -> PersianStrings
        "ru" -> RussianStrings
        else -> EnglishStrings
    }

    // Create a matching MaterialTheme darkColorScheme
    val colorScheme = darkColorScheme(
        primary = resolvedColors.primary,
        secondary = resolvedColors.accentCard,
        tertiary = resolvedColors.border,
        background = resolvedColors.background,
        surface = resolvedColors.card,
        onPrimary = resolvedColors.background,
        onSecondary = resolvedColors.textPrimary,
        onBackground = resolvedColors.textPrimary,
        onSurface = resolvedColors.textPrimary
    )

    CompositionLocalProvider(
        LocalAppColors provides resolvedColors,
        LocalAppStrings provides resolvedStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
