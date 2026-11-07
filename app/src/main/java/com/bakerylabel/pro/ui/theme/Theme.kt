package com.bakerylabel.pro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Maroon = Color(0xFF7A1F2B)
val MaroonDark = Color(0xFF4E0F18)
val MaroonDeep = Color(0xFF2E070D)
val Gold = Color(0xFFD4A944)
val GoldLight = Color(0xFFF1D78A)
val Cream = Color(0xFFFFF8EC)
val CreamDark = Color(0xFFF3E6CF)
val Chocolate = Color(0xFF4A2C21)
val Ink = Color(0xFF121010)
val InkSoft = Color(0xFF1E1A19)

private val LightColors = lightColorScheme(
    primary = Maroon,
    onPrimary = Cream,
    primaryContainer = Color(0xFFF6DADD),
    onPrimaryContainer = MaroonDeep,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFF8EBC6),
    onSecondaryContainer = Chocolate,
    tertiary = Chocolate,
    onTertiary = Cream,
    background = Cream,
    onBackground = Chocolate,
    surface = Color(0xFFFFFCF6),
    onSurface = Chocolate,
    surfaceVariant = CreamDark,
    onSurfaceVariant = Color(0xFF6B5045),
    outline = Color(0xFFBFA78C),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    onPrimary = MaroonDeep,
    primaryContainer = MaroonDark,
    onPrimaryContainer = GoldLight,
    secondary = Gold,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A2A12),
    onSecondaryContainer = GoldLight,
    tertiary = Color(0xFFE1C2A8),
    onTertiary = Ink,
    background = Ink,
    onBackground = Color(0xFFF2E6D6),
    surface = InkSoft,
    onSurface = Color(0xFFF2E6D6),
    surfaceVariant = Color(0xFF2C2523),
    onSurfaceVariant = Color(0xFFD7C2AE),
    outline = Color(0xFF6E5A4C),
    error = Color(0xFFF2B8B5),
)

private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 0.2.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val HeroGradient = Brush.linearGradient(listOf(MaroonDeep, Maroon, Color(0xFF9A2E3C)))
val GoldGradient = Brush.linearGradient(listOf(Color(0xFFB8892B), Gold, GoldLight))

@Composable
fun BakeryTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
