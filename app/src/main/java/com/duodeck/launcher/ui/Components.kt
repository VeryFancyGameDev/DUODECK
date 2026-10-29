package com.duodeck.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.duodeck.launcher.data.Game
import com.duodeck.launcher.data.RaProgress
import com.duodeck.launcher.data.Systems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Themed wallpaper: user image if set, otherwise the theme's pattern (dots, lines, grid, bubbles). */
@Composable
fun DotBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val duo = LocalDuo.current
    Box(modifier.background(duo.bg)) {
        if (duo.wallpaper != null) {
            AsyncImage(
                model = duo.wallpaper, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().background(duo.bgBottom.copy(alpha = duo.dim.coerceIn(0f, 0.95f))))
        } else {
            Canvas(Modifier.matchParentSize()) { drawPattern(duo) }
        }
        content()
    }
}

private fun DrawScope.drawPattern(duo: Duo) {
    when (duo.pattern) {
        "none" -> Unit
        "lines" -> {
            val step = 20.dp.toPx()
            var y = step
            while (y < size.height) {
                drawLine(duo.dots, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }
        }
        "grid" -> {
            val step = 30.dp.toPx()
            var y = step
            while (y < size.height) { drawLine(duo.dots, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
            var x = step
            while (x < size.width) { drawLine(duo.dots, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
        }
        "bubbles" -> {
            val r = Random(7)
            repeat(24) {
                drawCircle(
                    duo.dots, r.nextFloat() * 30.dp.toPx() + 8.dp.toPx(),
                    Offset(r.nextFloat() * size.width, r.nextFloat() * size.height),
                )
            }
        }
        else -> {
            val step = 26.dp.toPx()
            val radius = 2.6.dp.toPx()
            var y = step / 2
            var row = 0
            while (y < size.height + step) {
                var x = if (row % 2 == 0) step / 2 else step
                while (x < size.width + step) {
                    drawCircle(duo.dots, radius, Offset(x, y))
                    x += step
                }
                y += step; row++
            }
        }
    }
}

/** Squishy press animation used on every tappable thing, with optional long-press. */
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "bounce",
    )
    val tap by rememberUpdatedState(onClick)
    val longTap by rememberUpdatedState(onLongClick)
    return this
        .scale(s)
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try { tryAwaitRelease() } finally { pressed = false }
                },
                onLongPress = { longTap?.invoke() },
                onTap = { if (enabled) tap() },
            )
        }
}

@Composable
fun StatusStrip(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val duo = LocalDuo.current
    var now by remember { mutableStateOf(Date()) }
    var battery by remember { mutableIntStateOf(batteryPercent(ctx)) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); battery = batteryPercent(ctx); delay(20_000) }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
            color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now),
            color = duo.muted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(width = 26.dp, height = 12.dp).clip(RoundedCornerShape(4.dp))
            .border(1.5.dp, duo.muted, RoundedCornerShape(4.dp)).padding(2.dp)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(battery / 100f)
                .clip(RoundedCornerShape(2.dp))
                .background(if (battery <= 20) Color(0xFFE5484D) else duo.accent))
        }
        Text("$battery%", color = duo.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun batteryPercent(ctx: Context): Int {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 100
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    return (level * 100 / scale).coerceIn(0, 100)
}

/** Cover art if we have it, otherwise a cheerful gradient tile with the game's initials. Caller sets the shape/aspect. */
@Composable
fun Cover(game: Game, artUrl: String?, modifier: Modifier = Modifier, corner: Dp = 14.dp, showSystem: Boolean = true) {
    val system = Systems.get(game.systemId)
    val a = system?.colorA ?: Color(0xFF29B6F6)
    val b = system?.colorB ?: Color(0xFF81D4FA)
    val shape = RoundedCornerShape(corner)
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(a, b)))) {
        if (artUrl != null) {
            AsyncImage(model = artUrl, contentDescription = game.title,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        } else {
            Column(Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(initials(game.title), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                Text(game.title, color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        // glossy highlight, like the 3DS icons
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f).background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Transparent))))
        if (showSystem) {
            Text(system?.short ?: "", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp).clip(PillShape)
                    .background(Color.Black.copy(alpha = 0.38f)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

private fun initials(t: String): String =
    t.split(' ', '-', ':').filter { it.isNotBlank() && it.first().isLetterOrDigit() }
        .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** The launcher icon of an installed app. */
@Composable
fun AppIcon(pkg: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(160, 160).asImageBitmap() }.getOrNull()
        }
    }
    bmp?.let { Image(it, null, modifier) }
}

/** Round glossy button — the little bubble icons on the 3DS home menu. */
@Composable
fun GlossyIcon(
    icon: ImageVector, label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, active: Boolean = false, size: Dp = 46.dp,
) {
    val duo = LocalDuo.current
    val fill = if (active) duo.accentBrush else Brush.verticalGradient(listOf(duo.surfaceHigh, duo.surface))
    Box(modifier.size(size).bounceClick(onClick = onClick)
        .clip(CircleShape).background(fill).border(1.dp, duo.stroke, CircleShape),
        contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth(0.8f).fillMaxHeight(0.45f)
            .padding(top = 3.dp).clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (active) 0.45f else 0.5f), Color.Transparent))))
        Icon(icon, contentDescription = label, tint = if (active) Color.White else duo.text,
            modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null) {
    val duo = LocalDuo.current
    val bg = if (selected) Brush.verticalGradient(listOf(tint?.copy(alpha = 0.85f) ?: duo.accent2, tint ?: duo.accent))
    else Brush.verticalGradient(listOf(duo.surface, duo.surfaceHigh))
    Text(text, color = if (selected) Color.White else duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
        maxLines = 1,
        modifier = modifier.bounceClick(onClick = onClick).clip(PillShape).background(bg)
            .border(1.dp, duo.stroke, PillShape).padding(horizontal = 14.dp, vertical = 7.dp))
}

@Composable
fun RaBadge(p: RaProgress, modifier: Modifier = Modifier) {
    val gold = Color(0xFFF5B301)
    Row(modifier.clip(PillShape).background(Color.Black.copy(alpha = 0.55f))
        .padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🏆", fontSize = 9.sp)
        Spacer(Modifier.width(3.dp))
        Text("${p.awarded}/${p.max}", color = gold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    Text(text.uppercase(), color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp,
        letterSpacing = 1.2.sp, modifier = modifier.padding(top = 18.dp, bottom = 6.dp))
}

/** Rounded card used inside dialogs and settings. */
@Composable
fun DuoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val duo = LocalDuo.current
    Column(modifier.fillMaxWidth().clip(CardShape).background(duo.surface)
        .border(1.dp, duo.stroke, CardShape).padding(16.dp), content = content)
}

/** Centered popup with the app's look. */
@Composable
fun DuoDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val duo = LocalDuo.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(duo.surface)
                .border(1.dp, duo.stroke, RoundedCornerShape(24.dp)).padding(18.dp),
        ) {
            Text(title, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
