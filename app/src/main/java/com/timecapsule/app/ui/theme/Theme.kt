package com.timecapsule.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* Paleta del logo: cielo nocturno, coral, crema y dorado. Cámbiala aquí. */
val Night = Color(0xFF1A1236)
val NightSurface = Color(0xFF261B4A)
val NightRaised = Color(0xFF32255E)
val Coral = Color(0xFFFF8A7A)
val Rose = Color(0xFFF25F8C)
val Cream = Color(0xFFF6EFE2)
val Gold = Color(0xFFFFD98A)
val Muted = Color(0xFFB7AED6)

val CapsuleGradient = Brush.linearGradient(listOf(Coral, Rose))
val LockedGradient = Brush.linearGradient(listOf(Color(0xFF3B2A6B), Color(0xFF231848)))

private val scheme = darkColorScheme(
    primary = Coral, onPrimary = Color(0xFF2B0F17),
    secondary = Gold, onSecondary = Color(0xFF2A1F00),
    tertiary = Rose,
    background = Night, onBackground = Cream,
    surface = NightSurface, onSurface = Cream,
    surfaceVariant = NightRaised, onSurfaceVariant = Muted,
    surfaceContainer = NightSurface, surfaceContainerHigh = NightRaised,
    outline = Color(0xFF4A3D7A),
    error = Color(0xFFFF6B6B),
)

private val serif = FontFamily.Serif
private val type = Typography(
    displaySmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
)

@Composable
fun TimeCapsuleTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
