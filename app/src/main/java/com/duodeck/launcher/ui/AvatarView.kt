package com.duodeck.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.duodeck.launcher.data.Avatar
import kotlin.random.Random

/** Option counts and palettes for the avatar maker. All art here is drawn in code (original). */
object AvatarParts {
    val skins = listOf(0xFFFFE1CC, 0xFFF9D2B0, 0xFFEFB98A, 0xFFD9955F, 0xFFB57440, 0xFF8D5524, 0xFF6B3E1E, 0xFFBFE3C0, 0xFFB8C7FF)
        .map { Color(it) }
    val hairColors = listOf(
        0xFF1B1B1B, 0xFF3B2314, 0xFF6B4423, 0xFF8A3B1F, 0xFFD9B24C, 0xFFC8561E,
        0xFF9AA0A6, 0xFFEDEDED, 0xFFF06292, 0xFF4FC3F7, 0xFF9575CD, 0xFF66BB6A,
    ).map { Color(it) }
    val shirts = listOf(
        0xFF29B6F6, 0xFFEF5350, 0xFF66BB6A, 0xFFFFCA28, 0xFFAB47BC,
        0xFFFF7043, 0xFF26A69A, 0xFF5C6BC0, 0xFFEC407A, 0xFF37474F,
    ).map { Color(it) }
    val backgrounds = listOf(
        0xFFBDE6FF, 0xFFFFD6E8, 0xFFD5F5E3, 0xFFFFE9B8, 0xFFE3D8FF, 0xFFFFD9C7, 0xFFCFD8DC, 0xFF263238,
    ).map { Color(it) }

    const val HEADS = 3
    const val HAIRS = 7
    const val EYES = 4
    const val BROWS = 4
    const val NOSES = 3
    const val MOUTHS = 4
    const val GLASSES = 4
    const val BEARDS = 4

    fun random(name: String): Avatar {
        val r = Random(System.nanoTime())
        return Avatar(
            name = name,
            head = r.nextInt(HEADS), skin = r.nextInt(skins.size), hair = r.nextInt(HAIRS),
            hairColor = r.nextInt(hairColors.size), eyes = r.nextInt(EYES), brows = r.nextInt(BROWS),
            nose = r.nextInt(NOSES), mouth = r.nextInt(MOUTHS), glasses = if (r.nextInt(4) == 0) r.nextInt(1, GLASSES) else 0,
            beard = if (r.nextInt(5) == 0) r.nextInt(1, BEARDS) else 0, cheeks = r.nextBoolean(),
            shirt = r.nextInt(shirts.size), bg = r.nextInt(backgrounds.size),
        )
    }
}

/** A bust-style avatar. Fills a square; clip it with a shape in the caller. */
@Composable
fun AvatarView(a: Avatar, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawAvatar(a) }
}

private fun DrawScope.drawAvatar(a: Avatar) {
    val u = size.minDimension / 100f
    val ox = (size.width - 100f * u) / 2f
    val oy = (size.height - 100f * u) / 2f
    fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
    fun sz(w: Float, h: Float) = Size(w * u, h * u)
    fun line(w: Float) = Stroke(width = w * u, cap = StrokeCap.Round)

    val skin = AvatarParts.skins[a.skin.coerceIn(0, AvatarParts.skins.lastIndex)]
    val shade = lerp(skin, Color(0xFF7A3E1F), 0.22f)
    val hc = AvatarParts.hairColors[a.hairColor.coerceIn(0, AvatarParts.hairColors.lastIndex)]
    val ink = Color(0xFF262626)
    val cx = 50f
    val cy = 46f
    val (rx, ry) = when (a.head) { 1 -> 26f to 35f; 2 -> 33f to 29f; else -> 29f to 31f }

    // background
    drawRect(AvatarParts.backgrounds[a.bg.coerceIn(0, AvatarParts.backgrounds.lastIndex)])

    fun cap(extra: Float = 0f) = drawArc(
        hc, 180f, 180f, true, p(cx - rx - 2 - extra, cy - ry - 5 - extra), sz(2 * rx + 4 + 2 * extra, ry * 1.35f + extra),
    )

    // ---- hair behind head ----
    when (a.hair) {
        3 -> drawRoundRect(hc, p(cx - rx - 5, cy - ry + 2), sz(2 * rx + 10, 2 * ry + 22), CornerRadius(18f * u))
        4 -> drawRoundRect(hc, p(cx - rx - 4, cy - ry + 2), sz(2 * rx + 8, 2 * ry - 4), CornerRadius(16f * u))
        6 -> drawCircle(hc, (rx + 9f) * u, p(cx, cy - 6))
    }

    // ---- body ----
    drawRoundRect(AvatarParts.shirts[a.shirt.coerceIn(0, AvatarParts.shirts.lastIndex)],
        p(16f, 82f), sz(68f, 44f), CornerRadius(20f * u))
    drawRect(shade, p(42f, 70f), sz(16f, 16f))

    // ---- head ----
    drawCircle(skin, 5f * u, p(cx - rx, cy + 3))
    drawCircle(skin, 5f * u, p(cx + rx, cy + 3))
    drawOval(skin, p(cx - rx, cy - ry), sz(2 * rx, 2 * ry))

    // ---- beard (behind the features) ----
    when (a.beard) {
        2 -> drawOval(hc, p(cx - 5.5f, cy + ry - 11), sz(11f, 10f))
        3 -> {
            drawArc(hc, 0f, 180f, true, p(cx - rx + 0.5f, cy - ry + 0.5f), sz(2 * rx - 1, 2 * ry - 1))
            drawOval(skin, p(cx - 10f, cy + 10f), sz(20f, 15f))
        }
    }

    if (a.cheeks) {
        val blush = Color(0xFFFF8A80).copy(alpha = 0.38f)
        drawCircle(blush, 4.6f * u, p(cx - rx * 0.58f, cy + 12))
        drawCircle(blush, 4.6f * u, p(cx + rx * 0.58f, cy + 12))
    }

    // ---- nose ----
    when (a.nose) {
        0 -> drawOval(shade, p(cx - 1.8f, cy + 8), sz(3.6f, 3f))
        1 -> {
            drawLine(shade, p(cx, cy + 3), p(cx - 1.5f, cy + 10), 1.6f * u, StrokeCap.Round)
            drawLine(shade, p(cx - 1.5f, cy + 10), p(cx + 1.6f, cy + 10.5f), 1.6f * u, StrokeCap.Round)
        }
        else -> drawArc(shade, 20f, 140f, false, p(cx - 4f, cy + 5.5f), sz(8f, 6f), style = line(1.6f))
    }

    // ---- mustache ----
    if (a.beard == 1 || a.beard == 3) drawOval(hc, p(cx - 8f, cy + 12.5f), sz(16f, 5f))

    // ---- mouth ----
    val mouth = Color(0xFF8A3A3A)
    when (a.mouth) {
        0 -> drawArc(mouth, 20f, 140f, false, p(cx - 7f, cy + 12f), sz(14f, 10f), style = line(2.2f))
        1 -> {
            drawArc(Color(0xFF7A2B2B), 0f, 180f, true, p(cx - 6.5f, cy + 14f), sz(13f, 11f))
            drawArc(Color(0xFFE57373), 20f, 140f, true, p(cx - 3.5f, cy + 19.5f), sz(7f, 5f))
        }
        2 -> drawLine(mouth, p(cx - 5f, cy + 19f), p(cx + 5f, cy + 19f), 2.2f * u, StrokeCap.Round)
        else -> {
            drawArc(mouth, 0f, 180f, false, p(cx - 7f, cy + 14f), sz(7f, 6f), style = line(2f))
            drawArc(mouth, 0f, 180f, false, p(cx, cy + 14f), sz(7f, 6f), style = line(2f))
        }
    }

    // ---- eyes ----
    val ey = cy + 3f
    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * 11.5f
        when (a.eyes) {
            0 -> {
                drawOval(ink, p(ex - 2.6f, ey - 3.6f), sz(5.2f, 7.2f))
                drawCircle(Color.White, 1.2f * u, p(ex - 0.8f, ey - 1.8f))
            }
            1 -> {
                drawOval(Color.White, p(ex - 4.2f, ey - 4.8f), sz(8.4f, 9.6f))
                drawOval(ink, p(ex - 4.2f, ey - 4.8f), sz(8.4f, 9.6f), style = line(0.9f))
                drawCircle(ink, 3.2f * u, p(ex, ey))
                drawCircle(Color.White, 1.1f * u, p(ex - 1f, ey - 1.2f))
            }
            2 -> drawArc(ink, 200f, 140f, false, p(ex - 4.2f, ey - 3f), sz(8.4f, 8f), style = line(2.2f))
            else -> drawLine(ink, p(ex - 4f, ey), p(ex + 4f, ey), 2.4f * u, StrokeCap.Round)
        }
    }

    // ---- brows ----
    val by = cy - 8f
    for (side in listOf(-1f, 1f)) {
        val ex = cx + side * 11.5f
        when (a.brows) {
            1 -> drawLine(hc, p(ex - 5f, by), p(ex + 5f, by - 0.6f), 2.2f * u, StrokeCap.Round)
            2 -> drawLine(hc, p(ex - 5f * side, by - 2f), p(ex + 5f * side, by + 1.5f), 2.2f * u, StrokeCap.Round)
            3 -> drawArc(hc, 200f, 140f, false, p(ex - 5f, by - 4f), sz(10f, 8f), style = line(2f))
        }
    }

    // ---- glasses ----
    val glass = Color(0xFF263238)
    when (a.glasses) {
        1 -> {
            for (side in listOf(-1f, 1f)) drawCircle(glass, 8.5f * u, p(cx + side * 11.5f, ey), style = line(1.8f))
            drawLine(glass, p(cx - 3f, ey), p(cx + 3f, ey), 1.8f * u, StrokeCap.Round)
        }
        2 -> {
            for (side in listOf(-1f, 1f))
                drawRoundRect(glass, p(cx + side * 11.5f - 8.5f, ey - 6.5f), sz(17f, 13f), CornerRadius(3f * u), style = line(1.8f))
            drawLine(glass, p(cx - 3f, ey - 1f), p(cx + 3f, ey - 1f), 1.8f * u, StrokeCap.Round)
        }
        3 -> {
            for (side in listOf(-1f, 1f))
                drawRoundRect(glass.copy(alpha = 0.92f), p(cx + side * 11.5f - 8.5f, ey - 6f), sz(17f, 12f), CornerRadius(4f * u))
            drawLine(glass, p(cx - 3f, ey - 2f), p(cx + 3f, ey - 2f), 2f * u, StrokeCap.Round)
        }
    }

    // ---- hair in front ----
    when (a.hair) {
        1, 3, 4 -> cap()
        2 -> {
            cap()
            val top = cy - ry
            val path = Path()
            for (i in 0..4) {
                val x = cx - 24f + i * 12f
                val a1 = p(x - 6.5f, top + 5f)
                val a2 = p(x, top - 9f - (i % 2) * 4f)
                val a3 = p(x + 6.5f, top + 5f)
                path.moveTo(a1.x, a1.y)
                path.lineTo(a2.x, a2.y)
                path.lineTo(a3.x, a3.y)
                path.close()
            }
            drawPath(path, hc)
        }
        5 -> {
            drawCircle(hc, 9f * u, p(cx, cy - ry - 6))
            cap()
        }
        6 -> cap(extra = 2f)
    }
}
