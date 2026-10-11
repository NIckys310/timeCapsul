package com.timecapsule.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/* =========================================================
   Estilo "álbum de recuerdos vintage": papel crema, tinta sepia,
   lacre rojo y dorado. Hay versión de día (clara) y de noche (oscura),
   y una variante de alto contraste. Cambia los colores aquí.
   ========================================================= */
@Immutable
data class Palette(
    val bg: Color,          // fondo (papel)
    val surface: Color,     // tarjetas
    val raised: Color,      // tarjetas elevadas
    val ink: Color,         // texto principal (tinta)
    val muted: Color,       // texto secundario
    val wax: Color,         // lacre: color principal
    val waxDark: Color,
    val gold: Color,        // detalles dorados, cápsulas cerradas
    val olive: Color,       // abiertas / éxito
    val letter: Color,      // papel de la carta
    val letterInk: Color,
    val line: Color,
    val dark: Boolean,
)

private val DayPalette = Palette(
    bg = Color(0xFFF2E8D5), surface = Color(0xFFFBF5E9), raised = Color(0xFFEFE2C8),
    ink = Color(0xFF3B2E25), muted = Color(0xFF85725E), wax = Color(0xFFA63D2F), waxDark = Color(0xFF7A2A20),
    gold = Color(0xFFB8893A), olive = Color(0xFF5F7A45), letter = Color(0xFFFFFBF2), letterInk = Color(0xFF2E241C),
    line = Color(0xFFDCCBAA), dark = false,
)
/* Noche: madera de nogal cálida, tinta crema y bordes dorados */
private val NightPalette = Palette(
    bg = Color(0xFF2E1E14), surface = Color(0xFF3D2918), raised = Color(0xFF4E351F),
    ink = Color(0xFFF7E8CC), muted = Color(0xFFD2B48A), wax = Color(0xFFE47A52), waxDark = Color(0xFFB8552F),
    gold = Color(0xFFF2C14E), olive = Color(0xFFB5CC72), letter = Color(0xFFF8ECD4), letterInk = Color(0xFF2E241C),
    line = Color(0xFFA67C3D), dark = true,
)
private fun Palette.highContrast() = copy(
    ink = if (dark) Color.White else Color.Black,
    muted = if (dark) Color(0xFFE0D4C0) else Color(0xFF3A2E22),
    line = if (dark) Color(0xFF8A7860) else Color(0xFF6E5A42),
)

val LocalPalette = staticCompositionLocalOf { DayPalette }

/* Nombres que ya usaban las pantallas: ahora cambian con el tema */
val Night: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
val NightSurface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val NightRaised: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.raised
val Cream: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
val Coral: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.wax
val Rose: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.waxDark
val Gold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.gold
val Olive: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.olive
val Wax: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.wax
val LetterPaper: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.letter
val LetterInk: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.letterInk
val Line: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line

/** Abierta: dorado con un toque de lacre. Cerrada: sobre sepia. */
val CapsuleGradient: Brush @Composable @ReadOnlyComposable get() = LocalPalette.current.let {
    Brush.linearGradient(listOf(it.gold.copy(alpha = .85f), it.wax.copy(alpha = .85f)))
}
val LockedGradient: Brush @Composable @ReadOnlyComposable get() = LocalPalette.current.let {
    Brush.linearGradient(listOf(it.raised, it.surface))
}

/* Tipografías: serif para títulos (libro antiguo), monoespaciada para fechas (máquina de escribir) */
val Typewriter = FontFamily.Monospace
private val serif = FontFamily.Serif
private val type = Typography(
    displaySmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    labelSmall = TextStyle(fontFamily = Typewriter, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

private fun Palette.scheme() = if (dark) darkColorScheme(
    primary = wax, onPrimary = Color(0xFFFFF4E8), secondary = gold, onSecondary = Color(0xFF2A1F00), tertiary = olive,
    background = bg, onBackground = ink, surface = surface, onSurface = ink,
    surfaceVariant = raised, onSurfaceVariant = muted, surfaceContainer = surface, surfaceContainerHigh = raised,
    surfaceContainerHighest = raised, outline = line, error = Color(0xFFFF7A6B),
) else lightColorScheme(
    primary = wax, onPrimary = Color(0xFFFFF4E8), secondary = gold, onSecondary = Color(0xFF2A1F00), tertiary = olive,
    background = bg, onBackground = ink, surface = surface, onSurface = ink,
    surfaceVariant = raised, onSurfaceVariant = muted, surfaceContainer = surface, surfaceContainerHigh = raised,
    surfaceContainerHighest = raised, outline = line, error = Color(0xFFB3261E),
)

/**
 * @param themeMode "system" (sigue al celular), "light" o "dark"
 * @param textScale 1.0 normal · 1.15 grande · 1.3 muy grande (se suma al tamaño del celular)
 */
@Composable
fun TimeCapsuleTheme(
    themeMode: String = "system",
    highContrast: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val base = if (dark) NightPalette else DayPalette
    val palette = if (highContrast) base.highContrast() else base
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(colorScheme = palette.scheme(), typography = type, content = content)
    }
}
