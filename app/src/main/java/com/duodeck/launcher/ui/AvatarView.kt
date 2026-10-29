package com.duodeck.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.duodeck.launcher.data.Avatar
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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

/** A shaded avatar bust turned by [yaw] degrees (0 = facing you). Fills a square; clip it in the caller. */
@Composable
fun AvatarView(a: Avatar, modifier: Modifier = Modifier, yaw: Float = 0f) {
    Canvas(modifier) { drawAvatar(a, yaw) }
}

/** The interactive version: it sways gently, and you can drag to spin it all the way round. */
@Composable
fun Avatar3D(a: Avatar, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        -16f, 16f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s",
    )
    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    dragging = false
                    scope.launch { spin.animateTo((spin.value / 360f).roundToInt() * 360f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
                },
                onDragCancel = { dragging = false },
            ) { change, drag ->
                change.consume()
                scope.launch { spin.snapTo(spin.value + drag.x * 0.8f) }
            }
        },
    ) { drawAvatar(a, spin.value + if (dragging) 0f else sway) }
}

private fun shade(c: Color, f: Float) = lerp(c, Color.Black, f)
private fun tint(c: Color, f: Float) = lerp(c, Color.White, f)

private fun DrawScope.drawAvatar(a: Avatar, yawDeg: Float) {
    val u = size.minDimension / 100f
    val ox = (size.width - 100f * u) / 2f
    val oy = (size.height - 100f * u) / 2f
    fun p(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
    fun sz(w: Float, h: Float) = Size(w * u, h * u)
    fun line(w: Float) = Stroke(width = w * u, cap = StrokeCap.Round)
    fun rad(d: Float) = (d * PI / 180.0).toFloat()

    val skin = AvatarParts.skins[a.skin.coerceIn(0, AvatarParts.skins.lastIndex)]
    val hc = AvatarParts.hairColors[a.hairColor.coerceIn(0, AvatarParts.hairColors.lastIndex)]
    val shirt = AvatarParts.shirts[a.shirt.coerceIn(0, AvatarParts.shirts.lastIndex)]
    val ink = Color(0xFF262626)
    val skinShade = lerp(skin, Color(0xFF7A3E1F), 0.22f)
    val cx = 50f
    val cy = 46f
    val (rx, ry) = when (a.head) { 1 -> 26f to 35f; 2 -> 33f to 29f; else -> 29f to 31f }
    val facing = cos(rad(yawDeg))            // 1 = toward viewer, -1 = turned away

    // ---- helpers for sphere projection ----
    fun theta(lon: Float) = rad(lon + yawDeg)
    fun vis(lon: Float) = cos(theta(lon))
    fun fx(lon: Float, dy: Float): Float {
        val lat = asin((-dy / ry).coerceIn(-0.97f, 0.97f))
        return cx + rx * sin(theta(lon)) * cos(lat)
    }
    /** Draw [block] (origin = feature centre, units of 1/100 box) at a face position, squashed by foreshortening. */
    fun feature(lon: Float, dy: Float, block: DrawScope.() -> Unit) {
        val c = vis(lon)
        if (c < 0.14f) return
        val pos = p(fx(lon, dy), cy + dy)
        withTransform({
            translate(pos.x, pos.y)
            scale(c, 1f, Offset.Zero)
        }) { block() }
    }
    fun L(x: Float, y: Float) = Offset(x * u, y * u)

    // ---- background + ground shadow ----
    drawRect(AvatarParts.backgrounds[a.bg.coerceIn(0, AvatarParts.backgrounds.lastIndex)])
    drawOval(Color.Black.copy(alpha = 0.12f), p(20f, 90f), sz(60f, 10f))

    // ---- back hair (long / bob / afro) sits behind the head when facing us, in front when turned away ----
    fun backHair() {
        val grad = Brush.radialGradient(listOf(tint(hc, 0.15f), hc, shade(hc, 0.3f)), p(cx - 10f, cy - 15f), 62f * u)
        when (a.hair) {
            3 -> drawRoundRect(grad, p(cx - rx - 5, cy - ry + 2), sz(2 * rx + 10, 2 * ry + 22), CornerRadius(18f * u))
            4 -> drawRoundRect(grad, p(cx - rx - 4, cy - ry + 2), sz(2 * rx + 8, 2 * ry - 4), CornerRadius(16f * u))
            6 -> drawCircle(grad, (rx + 10f) * u, p(cx, cy - 7))
        }
    }
    if (facing >= 0f) backHair()

    // ---- body (narrower as it turns) ----
    withTransform({ scale(0.6f + 0.4f * abs(facing), 1f, p(cx, 90f)) }) {
        drawRoundRect(
            Brush.verticalGradient(listOf(tint(shirt, 0.2f), shade(shirt, 0.15f)), p(0f, 82f).y, p(0f, 112f).y),
            p(16f, 82f), sz(68f, 44f), CornerRadius(20f * u),
        )
        drawRect(skinShade, p(42f, 70f), sz(16f, 16f))
    }

    // ---- ears (behind the head until they swing into view) ----
    fun ears(front: Boolean) {
        for (lon in listOf(-90f, 90f)) {
            val c = vis(lon)
            if ((c > 0.05f) != front) continue
            val x = fx(lon, 3f)
            withTransform({ translate(p(x, cy + 3f).x, p(x, cy + 3f).y); scale(0.35f + 0.65f * abs(c), 1f, Offset.Zero) }) {
                drawOval(skin, L(-3.5f, -5f), sz(7f, 10f))
            }
        }
    }
    ears(false)

    // ---- head: a lit sphere ----
    drawOval(
        Brush.radialGradient(
            0f to tint(skin, 0.35f), 0.55f to skin, 1f to shade(skin, 0.28f),
            center = p(cx - rx * 0.35f, cy - ry * 0.45f), radius = maxOf(rx, ry) * 1.7f * u,
        ),
        p(cx - rx, cy - ry), sz(2 * rx, 2 * ry),
    )
    ears(true)

    // ---- face features, projected onto the sphere ----
    // beard base
    when (a.beard) {
        2 -> feature(0f, ry - 11f) { drawOval(hc, L(-5.5f, -5f), sz(11f, 10f)) }
        3 -> {
            val path = Path()
            val steps = 14
            for (k in 0..steps) {
                val thd = -90f + 180f * k / steps
                val c = cos(rad(thd - yawDeg))        // 1 when this meridian is the face centre
                val latB = rad(-4f - 22f * c.coerceAtLeast(0f))
                val x = cx + rx * sin(rad(thd)) * cos(latB)
                val y = cy - ry * sin(latB)
                if (k == 0) path.moveTo(p(x, y).x, p(x, y).y) else path.lineTo(p(x, y).x, p(x, y).y)
            }
            val latEnd = rad(-4f)
            val startPhi = (180f / PI * (-(-latEnd))).toFloat()  // right edge angle (deg, clockwise)
            for (k in 0..12) {
                val phi = rad(-(startPhi) + (180f + 2f * startPhi) * k / 12f)
                val x = cx + rx * cos(phi); val y = cy + ry * sin(phi)
                path.lineTo(p(x, y).x, p(x, y).y)
            }
            path.close()
            drawPath(path, Brush.radialGradient(listOf(tint(hc, 0.1f), shade(hc, 0.3f)), p(cx, cy + 10f), 40f * u))
            feature(0f, 19f) { drawOval(skin, L(-9.5f, -7f), sz(19f, 14f)) }
        }
    }

    if (a.cheeks) {
        val blush = Color(0xFFFF6F61).copy(alpha = 0.32f)
        for (s in listOf(-1f, 1f)) feature(34f * s, 12f) { drawCircle(blush, 4.6f * u, Offset.Zero) }
    }

    // nose
    feature(0f, 9f) {
        when (a.nose) {
            0 -> drawOval(skinShade, L(-1.8f, -1.5f), sz(3.6f, 3f))
            1 -> {
                drawLine(skinShade, L(0f, -5.5f), L(-1.5f, 1.5f), 1.6f * u, StrokeCap.Round)
                drawLine(skinShade, L(-1.5f, 1.5f), L(1.6f, 2f), 1.6f * u, StrokeCap.Round)
            }
            else -> drawArc(skinShade, 20f, 140f, false, L(-4f, -3.5f), sz(8f, 6f), style = line(1.6f))
        }
    }

    // mustache
    if (a.beard == 1 || a.beard == 3) feature(0f, 13f) { drawOval(hc, L(-8f, -2.5f), sz(16f, 5f)) }

    // mouth
    val mouth = Color(0xFF8A3A3A)
    feature(0f, 19f) {
        when (a.mouth) {
            0 -> drawArc(mouth, 20f, 140f, false, L(-7f, -7f), sz(14f, 10f), style = line(2.2f))
            1 -> {
                drawArc(Color(0xFF7A2B2B), 0f, 180f, true, L(-6.5f, -5f), sz(13f, 11f))
                drawArc(Color(0xFFE57373), 20f, 140f, true, L(-3.5f, 0.5f), sz(7f, 5f))
            }
            2 -> drawLine(mouth, L(-5f, 0f), L(5f, 0f), 2.2f * u, StrokeCap.Round)
            else -> {
                drawArc(mouth, 0f, 180f, false, L(-7f, -5f), sz(7f, 6f), style = line(2f))
                drawArc(mouth, 0f, 180f, false, L(0f, -5f), sz(7f, 6f), style = line(2f))
            }
        }
    }

    // eyes + brows
    for (s in listOf(-1f, 1f)) {
        feature(24f * s, 3f) {
            when (a.eyes) {
                0 -> {
                    drawOval(ink, L(-2.6f, -3.6f), sz(5.2f, 7.2f))
                    drawCircle(Color.White, 1.2f * u, L(-0.8f, -1.8f))
                }
                1 -> {
                    drawOval(Color.White, L(-4.2f, -4.8f), sz(8.4f, 9.6f))
                    drawOval(ink, L(-4.2f, -4.8f), sz(8.4f, 9.6f), style = line(0.9f))
                    drawCircle(ink, 3.2f * u, Offset.Zero)
                    drawCircle(Color.White, 1.1f * u, L(-1f, -1.2f))
                }
                2 -> drawArc(ink, 200f, 140f, false, L(-4.2f, -3f), sz(8.4f, 8f), style = line(2.2f))
                else -> drawLine(ink, L(-4f, 0f), L(4f, 0f), 2.4f * u, StrokeCap.Round)
            }
        }
        feature(24f * s, -8f) {
            when (a.brows) {
                1 -> drawLine(hc, L(-5f, 0f), L(5f, -0.6f), 2.2f * u, StrokeCap.Round)
                2 -> drawLine(hc, L(-5f * s, -2f), L(5f * s, 1.5f), 2.2f * u, StrokeCap.Round)
                3 -> drawArc(hc, 200f, 140f, false, L(-5f, -4f), sz(10f, 8f), style = line(2f))
            }
        }
    }

    // glasses
    val glass = Color(0xFF263238)
    if (a.glasses != 0) {
        for (s in listOf(-1f, 1f)) {
            feature(24f * s, 3f) {
                when (a.glasses) {
                    1 -> drawCircle(glass, 8.5f * u, Offset.Zero, style = line(1.8f))
                    2 -> drawRoundRect(glass, L(-8.5f, -6.5f), sz(17f, 13f), CornerRadius(3f * u), style = line(1.8f))
                    else -> drawRoundRect(glass.copy(alpha = 0.92f), L(-8.5f, -6f), sz(17f, 12f), CornerRadius(4f * u))
                }
            }
        }
        feature(0f, 2f) { drawLine(glass, L(-3f, 0f), L(3f, 0f), 1.8f * u, StrokeCap.Round) }
    }

    // ---- hair cap: its lower edge follows a hairline that is lower at the back and sides ----
    fun hairLat(lonFromFace: Float): Float {
        val c = cos(rad(lonFromFace))
        val long = a.hair == 3 || a.hair == 4
        return if (c >= 0f) (if (long) 8f else 12f) + 20f * c else (if (long) -4f else 12f) + (if (long) 16f else 30f) * c
    }
    fun cap(extra: Float) {
        val path = Path()
        val n = 16
        for (k in 0..n) {
            val thd = -90f + 180f * k / n               // angle as seen from the viewer
            val lat = rad(hairLat(thd - yawDeg))
            val x = cx + (rx + extra) * sin(rad(thd)) * cos(lat)
            val y = cy - (ry + extra) * sin(lat)
            if (k == 0) path.moveTo(p(x, y).x, p(x, y).y) else path.lineTo(p(x, y).x, p(x, y).y)
        }
        val lr = hairLat(90f - yawDeg)
        val ll = hairLat(-90f - yawDeg)
        val phi0 = -lr
        val phi1 = ll - 180f
        for (k in 1..14) {
            val phi = rad(phi0 + (phi1 - phi0) * k / 14f)
            val x = cx + (rx + extra) * cos(phi)
            val y = cy + (ry + extra) * sin(phi)
            path.lineTo(p(x, y).x, p(x, y).y)
        }
        path.close()
        drawPath(path, Brush.radialGradient(listOf(tint(hc, 0.25f), hc, shade(hc, 0.32f)), p(cx - 12f, cy - ry), 60f * u))
    }
    if (a.hair != 0) {
        if (facing < 0f) backHair()
        cap(if (a.hair == 6) 3.5f else 2.2f)
        if (a.hair == 2) {
            // spikes around the crown
            for (k in 0..6) {
                val lon = -75f + 25f * k
                val th = theta(lon)
                if (cos(th) < -0.2f) continue
                val bx = cx + (rx + 1f) * sin(th) * 0.92f
                val by = cy - ry * 0.84f - abs(sin(th)) * 2f
                val tip = p(bx + sin(th) * 4f, by - 13f - (k % 2) * 4f)
                val path = Path().apply {
                    moveTo(p(bx - 5.5f, by + 3f).x, p(bx - 5.5f, by + 3f).y)
                    lineTo(tip.x, tip.y)
                    lineTo(p(bx + 5.5f, by + 3f).x, p(bx + 5.5f, by + 3f).y)
                    close()
                }
                drawPath(path, shade(hc, 0.1f))
            }
        }
        if (a.hair == 5) {
            val bunX = cx + 0f
            drawCircle(Brush.radialGradient(listOf(tint(hc, 0.3f), hc, shade(hc, 0.3f)), p(bunX - 3f, cy - ry - 9f), 11f * u),
                9f * u, p(bunX, cy - ry - 5f))
        }
    }

    // ---- soft highlight so the head reads as a ball ----
    drawOval(Color.White.copy(alpha = 0.18f), p(cx - rx * 0.62f, cy - ry * 0.78f), sz(rx * 0.55f, ry * 0.32f))
}
