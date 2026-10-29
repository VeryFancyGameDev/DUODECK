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
import com.duodeck.launcher.data.Pal
import com.duodeck.launcher.data.ThemeDef
import com.duodeck.launcher.data.ThemeMode
import com.duodeck.launcher.data.Themes

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
    val pattern: String,
    val wallpaper: String?,
    val dim: Float,
) {
    val bg get() = Brush.verticalGradient(listOf(bgTop, bgBottom))
    val accentBrush get() = Brush.verticalGradient(listOf(accent2, accent))
}

fun duoFor(dark: Boolean, def: ThemeDef, wallpaper: String?, dim: Float): Duo {
    val p: Pal = if (dark) def.dark else def.light
    return Duo(
        dark = dark,
        bgTop = Color(p.bgTop), bgBottom = Color(p.bgBottom), dots = Color(p.dots),
        surface = Color(p.surface), surfaceHigh = Color(p.surfaceHigh),
        text = Color(p.text), muted = Color(p.muted),
        accent = Color(p.accent), accent2 = Color(p.accent2), stroke = Color(p.stroke),
        pattern = def.pattern, wallpaper = wallpaper, dim = dim,
    )
}

val LocalDuo = staticCompositionLocalOf { duoFor(false, Themes.builtin.first(), null, 0f) }

val CardShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(50)

private val Rounded = FontFamily.SansSerif

@Composable
fun DuoTheme(
    mode: ThemeMode,
    def: ThemeDef,
    wallpaper: String?,
    dim: Float,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val duo = duoFor(dark, def, wallpaper, dim)
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

/** Colour pairs for user-made groups. */
val GroupColors = listOf(
    Color(0xFF29B6F6) to Color(0xFF81D4FA),
    Color(0xFFEC407A) to Color(0xFFF8BBD0),
    Color(0xFF66BB6A) to Color(0xFFC5E1A5),
    Color(0xFFFFA726) to Color(0xFFFFE0B2),
    Color(0xFF7E57C2) to Color(0xFFD1C4E9),
    Color(0xFF26A69A) to Color(0xFFB2DFDB),
)
