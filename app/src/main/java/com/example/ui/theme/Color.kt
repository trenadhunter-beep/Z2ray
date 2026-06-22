package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Theme colors data class
data class AppColors(
    val background: Color,
    val card: Color,
    val accentCard: Color,
    val primary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean = true
)

// Handcrafted Themes Preset mapping
val BentoDarkTheme = AppColors(
    background = Color(0xFF131216),
    card = Color(0xFF1E1C22),
    accentCard = Color(0xFF2C2538),
    primary = Color(0xFFD0BCFF),
    border = Color(0xFF3E3A4A),
    textPrimary = Color(0xFFE6E1E5),
    textSecondary = Color(0xFF938F99)
)

val CyberpunkNeonTheme = AppColors(
    background = Color(0xFF0F0E13),
    card = Color(0xFF1B1824),
    accentCard = Color(0xFF281F38),
    primary = Color(0xFF00FFE0),
    border = Color(0xFF3A2B5E),
    textPrimary = Color(0xFFF0E6FF),
    textSecondary = Color(0xFFA592C4)
)

val EmeraldSecureTheme = AppColors(
    background = Color(0xFF0C1310),
    card = Color(0xFF131D18),
    accentCard = Color(0xFF1B2E24),
    primary = Color(0xFF10B981),
    border = Color(0xFF2D4E3F),
    textPrimary = Color(0xFFE2F0EA),
    textSecondary = Color(0xFF8BAE9D)
)

val MidnightBlackTheme = AppColors(
    background = Color(0xFF000000),
    card = Color(0xFF121212),
    accentCard = Color(0xFF1E1E1E),
    primary = Color(0xFF3B82F6),
    border = Color(0xFF2C2C2C),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA3A3A3)
)

val LocalAppColors = staticCompositionLocalOf { BentoDarkTheme }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

// Keep standard colors for compatibility / non-theming parts
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFF332D41)
val Pink80 = Color(0xFF4A4458)
val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Standard status indicators
val SecureGreen = Color(0xFF4ADE80)
val DangerRed = Color(0xFFF2B8B5)
val WarningOrange = Color(0xFFF59E0B)

// Backporting legacy cyber aliases to dynamic resolver where needed or just defining them as static fallback
val CyberBlack = Color(0xFF131216)
val CyberNavy = Color(0xFF1E1C22)
val CyberCard = Color(0xFF2C2538)
val CyberPrimary = Color(0xFFD0BCFF)
val CyberSecondary = Color(0xFFE6E1E5)
val CyberAccent = Color(0xFF4A4458)
val CyberTextPrimary = Color(0xFFE6E1E5)
val CyberTextSecondary = Color(0xFF938F99)

val BentoBlack = Color(0xFF1C1B1F)
val BentoNavy = Color(0xFF262529)
val BentoCard = Color(0xFF2B2930)
val BentoAccentCard = Color(0xFF332D41)
val BentoPrimary = Color(0xFFD0BCFF)
val BentoSecondary = Color(0xFFE6E1E5)
val BentoAccent = Color(0xFF4A4458)
val BentoBorder = Color(0xFF49454F)
val BentoTextPrimary = Color(0xFFE6E1E5)
val BentoTextSecondary = Color(0xFF938F99)
