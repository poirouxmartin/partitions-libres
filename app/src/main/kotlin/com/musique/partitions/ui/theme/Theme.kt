package com.musique.partitions.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// « papier et encre » palette (paper and ink)
private val LightColors = lightColorScheme(
    primary = Color(0xFF23395B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5F2),
    onPrimaryContainer = Color(0xFF14233D),
    secondary = Color(0xFFA0552A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E1D3),
    onSecondaryContainer = Color(0xFF4A2410),
    tertiary = Color(0xFF5C7A5A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE9DB),
    onTertiaryContainer = Color(0xFF1F3320),
    background = Color(0xFFFBF7EF),
    onBackground = Color(0xFF1F2430),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1F2430),
    surfaceVariant = Color(0xFFF1EADC),
    onSurfaceVariant = Color(0xFF5B5F6B),
    outline = Color(0xFFD8CFC0),
    outlineVariant = Color(0xFFE8E0D2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C1E8),
    onPrimary = Color(0xFF11243F),
    primaryContainer = Color(0xFF2C4366),
    onPrimaryContainer = Color(0xFFDDE5F2),
    secondary = Color(0xFFE0A27A),
    onSecondary = Color(0xFF3F1C08),
    secondaryContainer = Color(0xFF5A3720),
    onSecondaryContainer = Color(0xFFF3E1D3),
    tertiary = Color(0xFFA8C5A5),
    onTertiary = Color(0xFF16301A),
    tertiaryContainer = Color(0xFF2F4A30),
    onTertiaryContainer = Color(0xFFDDE9DB),
    background = Color(0xFF14181F),
    onBackground = Color(0xFFE8E4DA),
    surface = Color(0xFF1B2029),
    onSurface = Color(0xFFE8E4DA),
    surfaceVariant = Color(0xFF262C37),
    onSurfaceVariant = Color(0xFFB7BCC8),
    outline = Color(0xFF3A4150),
    outlineVariant = Color(0xFF2E3541),
)

private val base = Typography()
private val AppTypography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Serif),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
