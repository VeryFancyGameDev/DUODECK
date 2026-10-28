package com.duodeck.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duodeck.launcher.data.ThemeMode

data class Duo(
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val dots: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val stroke: Color,
) {
    val bg get() = Brush.verticalGradient(listOf(bgTop, bgBottom))
    val accentBrush get() = Brush.verticalGradient(listOf(accent2, accent))
}

val Accents = listOf(
    "Sky" to (Color(0xFF1FA8E0) to Color(0xFF6FD6FF)),
    "Bubblegum" to (Color(0xFFE8509A) to Color(0xFFFF9CCB)),
    "Mint" to (Color(0xFF1DB98A) to Color(0xFF7DEBC2)),
    "Sunny" to (Color(0xFFF29B1D) to Color(0xFFFFD36B)),
    "Grape" to (Color(0xFF7A55E0) to Color(0xFFB79CFF)),
)

fun duoFor(dark: Boolean, accentIndex: Int): Duo {
    val (a, a2) = Accents[accentIndex.coerceIn(0, Accents.lastIndex)].second
    return if (dark) Duo(
        dark = true,
        bgTop = Color(0xFF1B2540), bgBottom = Color(0xFF0E1426),
        dots = Color(0x14FFFFFF),
        surface = Color(0xFF243055), surfaceHigh = Color(0xFF2E3C6B),
        text = Color(0xFFF1F5FF), muted = Color(0xFF9FB0D8),
        accent = a, accent2 = a2, stroke = Color(0x33FFFFFF),
    ) else Duo(
        dark = false,
        bgTop = Color(0xFFF7FBFF), bgBottom = Color(0xFFDCEBF7),
        dots = Color(0x1A3D8EC9),
        surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFEAF3FB),
        text = Color(0xFF26324A), muted = Color(0xFF6F809E),
        accent = a, accent2 = a2, stroke = Color(0x1F26324A),
    )
}

val LocalDuo = staticCompositionLocalOf { duoFor(false, 0) }

val CardShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(50)

private val Rounded = FontFamily.SansSerif

@Composable
fun DuoTheme(mode: ThemeMode, accent: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val duo = duoFor(dark, accent)
    val scheme = if (dark) darkColorScheme(primary = duo.accent, background = duo.bgBottom, surface = duo.surface, onSurface = duo.text)
    else lightColorScheme(primary = duo.accent, background = duo.bgBottom, surface = duo.surface, onSurface = duo.text)
    CompositionLocalProvider(LocalDuo provides duo) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                titleLarge = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                titleMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                bodyMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Medium, fontSize = 14.sp),
                labelMedium = TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = 12.sp),
            ),
            content = content,
        )
    }
}
