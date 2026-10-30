package com.duodeck.launcher.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.Badge
import com.duodeck.launcher.data.Badges
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** A round rarity-coloured medallion with the badge's glyph. */
@Composable
fun BadgeMedallion(b: Badge, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false) {
    val shape = CircleShape
    Box(modifier.size(size).clip(shape)
        .background(if (locked) Brush.verticalGradient(listOf(Color(0xFFB0BEC5), Color(0xFF78909C)))
        else Brush.linearGradient(listOf(b.rarity.b, b.rarity.a)))
        .border((size.value * 0.06f).dp, if (locked) Color(0xFF607D8B) else b.rarity.a, shape),
        contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth(0.7f).fillMaxHeight(0.4f).padding(top = (size.value * 0.06f).dp)
            .clip(RoundedCornerShape(50)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent))))
        Text(if (locked) "?" else b.glyph, fontSize = (size.value * 0.5f).sp, color = if (locked) Color.White else Color.Unspecified,
            fontWeight = FontWeight.ExtraBold)
    }
}

/** Badge Arcade: spend Play Coins on a claw machine to win collectible badges, then show them off on the top screen. */
@Composable
fun ArcadeScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val user by vm.user.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Badge Arcade", vm)
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinIcon(Modifier, 22.dp)
                Text("${user.coins}", color = duo.text, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Pill("Claw", tab == 0, { tab = 0 })
                Pill("Badge Book ${user.badges.size}/${Badges.all.size}", tab == 1, { tab = 1 })
            }
            Spacer(Modifier.height(8.dp))
            if (tab == 0) ClawMachine(vm) else BadgeBook(vm)
        }
    }
}

@Composable
private fun ClawMachine(vm: LibraryViewModel) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf("idle") }   // idle | drop | result
    val clawX = remember { Animatable(0.1f) }
    val clawY = remember { Animatable(0f) }
    fun newCaps(): List<Float> = (0..8).shuffled().take(4).map { 0.1f + it * 0.1f }
    var caps by remember { mutableStateOf(newCaps()) }
    val capColors = remember { listOf(Color(0xFFFF7043), Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFFFFCA28)) }
    var carried by remember { mutableStateOf<Int?>(null) }
    var won by remember { mutableStateOf<Badge?>(null) }
    var missed by remember { mutableStateOf(false) }

    LaunchedEffect(phase) {
        if (phase == "idle") {
            while (true) {
                clawX.animateTo(0.92f, tween(1500, easing = LinearEasing))
                clawX.animateTo(0.08f, tween(1500, easing = LinearEasing))
            }
        }
    }

    fun drop() {
        if (phase != "idle") return
        if (!vm.payClaw()) { Toast.makeText(ctx, "You need ${Badges.CLAW_COST} Play Coins. Walk or play games to earn more!", Toast.LENGTH_SHORT).show(); return }
        phase = "drop"
        scope.launch {
            clawY.animateTo(1f, tween(750, easing = LinearEasing))
            val hit = caps.indexOfFirst { abs(it - clawX.value) < 0.05f }
            if (hit >= 0) carried = hit
            clawY.animateTo(0f, tween(850, easing = LinearEasing))
            if (hit >= 0) { val b = Badges.roll(Random); vm.addBadge(b.id); won = b } else missed = true
            carried = null
            phase = "result"
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val cabinet = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1.05f).clip(cabinet)
            .background(Brush.verticalGradient(listOf(duo.surface, duo.surfaceHigh)))
            .border(5.dp, Brush.verticalGradient(listOf(duo.accent2, duo.accent)), cabinet)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val railY = h * 0.07f
                drawLine(duo.muted.copy(alpha = 0.5f), Offset(w * 0.04f, railY), Offset(w * 0.96f, railY), 6.dp.toPx(), StrokeCap.Round)
                val cx = w * (0.0f + clawX.value)
                val headY = railY + clawY.value * (h * 0.62f) + 14.dp.toPx()
                drawLine(duo.text.copy(alpha = 0.7f), Offset(cx, railY), Offset(cx, headY), 3.dp.toPx())
                val open = if (phase == "drop" && clawY.value >= 1f && carried == null) 1f else 0f
                val grip = 16.dp.toPx() * (1f - 0.35f * (if (carried != null) 1f else 0f)) + open * 6.dp.toPx()
                drawCircle(duo.text, 7.dp.toPx(), Offset(cx, headY))
                for (sx in listOf(-1f, 1f)) {
                    drawLine(duo.text, Offset(cx, headY), Offset(cx + sx * grip, headY + 22.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)
                    drawLine(duo.text, Offset(cx + sx * grip, headY + 22.dp.toPx()), Offset(cx + sx * grip * 0.55f, headY + 34.dp.toPx()), 5.dp.toPx(), StrokeCap.Round)
                }
                val floorY = h * 0.84f
                drawRect(duo.muted.copy(alpha = 0.18f), Offset(0f, floorY + 14.dp.toPx()), androidx.compose.ui.geometry.Size(w, h))
                val r = w * 0.042f
                caps.forEachIndexed { i, fx ->
                    val col = capColors[i % capColors.size]
                    val pos = if (carried == i) Offset(cx, headY + 34.dp.toPx() + r) else Offset(w * fx, floorY)
                    drawCircle(col, r, pos)
                    drawCircle(Color.White.copy(alpha = 0.75f), r * 0.55f, Offset(pos.x - r * 0.25f, pos.y - r * 0.3f))
                    drawCircle(Color.Black.copy(alpha = 0.18f), r, pos, style = Stroke(1.5.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Line the claw up over a capsule and press DROP. Each try costs ${Badges.CLAW_COST} Play Coins.",
            color = duo.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(if (phase == "idle") "DROP  (${Badges.CLAW_COST} coins)" else if (phase == "drop") "…" else "AGAIN",
            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
            modifier = Modifier.bounceClick(enabled = phase != "drop") {
                if (phase == "result") { won = null; missed = false; caps = newCaps(); scope.launch { clawY.snapTo(0f); phase = "idle" } }
                else drop()
            }.clip(PillShape).background(duo.accentBrush).padding(horizontal = 36.dp, vertical = 14.dp))
        if (user.clawPlays > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Tries so far: ${user.clawPlays}", color = duo.muted, fontSize = 11.sp)
        }
    }

    won?.let { b ->
        DuoDialog("You got a badge!", onDismiss = { won = null }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BadgeMedallion(b, 96.dp)
                Spacer(Modifier.height(8.dp))
                Text(b.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(b.rarity.label, color = b.rarity.a, fontWeight = FontWeight.ExtraBold)
                if ((user.badges[b.id] ?: 0) > 1) Text("Duplicate! Sell extras in the Badge Book.", color = duo.muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Pill("Nice!", true, { won = null })
            }
        }
    }
    if (missed) {
        DuoDialog("So close!", onDismiss = { missed = false }) {
            Text("The claw came up empty. Try again!", color = duo.muted)
            Spacer(Modifier.height(10.dp))
            Pill("OK", true, { missed = false })
        }
    }
}

@Composable
private fun BadgeBook(vm: LibraryViewModel) {
    val duo = LocalDuo.current
    val user by vm.user.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<Badge?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text("Tap a badge to pin it (up to 5) on the top screen. Pinned: ${user.pins.size}/5",
            color = duo.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        LazyVerticalGrid(GridCells.Adaptive(76.dp), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Badges.all) { b ->
                val n = user.badges[b.id] ?: 0
                Column(Modifier.bounceClick(enabled = n > 0) { open = b }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        BadgeMedallion(b, 64.dp, locked = n == 0)
                        if (b.id in user.pins) Text("📌", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd))
                        if (n > 1) Text("×$n", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.BottomEnd).clip(PillShape).background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 5.dp))
                    }
                    Text(if (n > 0) b.name else "???", color = duo.text, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }

    open?.let { b ->
        val n = user.badges[b.id] ?: 0
        DuoDialog(b.name, onDismiss = { open = null }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BadgeMedallion(b, 90.dp)
                Text("${b.rarity.label} · you own $n", color = duo.muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (b.id in user.pins) "Unpin" else "Pin to top screen", true, { vm.togglePin(b.id) })
                    if (n > 1) Pill("Sell 1 duplicate", false, { vm.sellDuplicate(b.id) })
                }
            }
        }
    }
}
