package com.duodeck.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.*
import com.duodeck.launcher.data.Badges
import com.duodeck.launcher.data.DiscordFeed
import com.duodeck.launcher.data.Emulators
import com.duodeck.launcher.data.Launcher
import com.duodeck.launcher.data.RetroAchievements
import com.duodeck.launcher.data.Systems
import kotlin.math.abs

/** The "top screen": a floating row of cubes for the highlighted tile and its neighbours, plus details. */
@Composable
fun TopScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val tile by vm.selected.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val byId = remember(games) { games.associateBy { it.id } }
    val env = TileEnv(art, progress, user.favorites, byId, false)

    val cur = tiles.indexOfFirst { it.key == tile?.key }.coerceAtLeast(0)
    val today = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val coinsToday = if (user.coinDay == today) user.coinsToday.coerceAtMost(10) else 0
    val discordMsgs by DiscordFeed.msgs.collectAsStateWithLifecycle()
    val discordUnread by DiscordFeed.unread.collectAsStateWithLifecycle()

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            HomeStatusBar(user.coins, Modifier.fillMaxWidth())
            discordMsgs.firstOrNull()?.takeIf { discordUnread > 0 }?.let { m ->
                Row(Modifier.padding(top = 4.dp).clip(PillShape).background(Color(0xFF5865F2).copy(alpha = 0.92f))
                    .bounceClick { vm.go(Screen.DISCORD) }.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("💬 ${m.author}: ${m.text}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (discordUnread > 1) Text("  +${discordUnread - 1}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }

            // ---- cube row ----
            BoxWithConstraints(Modifier.weight(1.1f).fillMaxWidth()) {
                val s = (maxHeight * 0.92f).coerceAtMost(maxWidth * 0.34f)
                if (tiles.isEmpty()) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DuoDeck", color = duo.accent, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (games.isEmpty()) "Pick your ROMs folder on the bottom screen" else "Nothing here yet",
                            color = duo.muted, fontSize = 13.sp)
                    }
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy((-s * 0.08f), Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically) {
                        for (off in -2..2) {
                            val t = tiles.getOrNull(cur + off)
                            val size = when (abs(off)) { 0 -> s; 1 -> s * 0.74f; else -> s * 0.56f }
                            if (t != null) {
                                HomeCube(t, off, size, env) { vm.select(t) }
                            } else Spacer(Modifier.size(size))
                        }
                    }
                }
            }

            // ---- details ----
            AnimatedContent(
                targetState = tile,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(0.9f).fillMaxWidth(), label = "info",
            ) { t ->
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    when (t) {
                        null, is AddTile -> Text("Choose something to play", color = duo.muted, fontSize = 14.sp)
                        is GameTile -> {
                            val g = t.game
                            val sys = Systems.get(g.systemId)
                            val ra = progress[RetroAchievements.normalise(g.title)]
                            val st = user.stats[g.id]
                            Text(g.title, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Text("${sys?.name ?: ""} · " + (if (st == null) "Not played yet" else "Played ${st.plays}× · ${relTime(st.last)}"),
                                color = duo.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                            if (ra != null && ra.max > 0) {
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth(0.55f).height(8.dp).clip(PillShape).background(duo.surfaceHigh)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(ra.awarded / ra.max.toFloat()).clip(PillShape)
                                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD36B), Color(0xFFF29B1D)))))
                                }
                                Text("🏆 ${ra.awarded}/${ra.max}", color = duo.muted, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayButton("PLAY") { vm.launchGame(g) }
                                val r = Launcher.resolve(ctx, g.systemId, g.id, user)
                                Text(
                                    if (r.pkg == null) "No emulator found"
                                    else (r.known?.label ?: r.pkg) +
                                        (if (r.pkg in Emulators.retroarchPkgs && r.core != null) " · ${r.core}" else ""),
                                    color = duo.muted, fontSize = 11.sp, maxLines = 2, modifier = Modifier.widthIn(max = 130.dp))
                            }
                        }
                        is GroupTile -> {
                            Text(t.group.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                            Text("Group · ${t.group.gameIds.size} game${if (t.group.gameIds.size == 1) "" else "s"}", color = duo.muted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            PlayButton("OPEN") { vm.activate(t) }
                        }
                        is SysTile -> {
                            Text(t.system.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                            Text("${t.count} game${if (t.count == 1) "" else "s"}", color = duo.muted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            PlayButton("OPEN") { vm.activate(t) }
                        }
                        is AppTile -> {
                            Text(t.app.label, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                            Text("App", color = duo.muted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            PlayButton("OPEN") { vm.activate(t) }
                        }
                    }
                }
            }

            // ---- button hints (tap them or use a gamepad) ----
            Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Hint("SELECT", "Emulator") {
                    (tile as? GameTile)?.let { vm.show(Dlg.EmuProfile("game:${it.game.id}", it.game.systemId, it.game.title)) }
                }
                Hint("Y", "Layout") { vm.cycleLayout() }
                Hint("X", "Menu") { tile?.let { vm.longPress(it) } }
                Spacer(Modifier.weight(1f))
                user.pins.mapNotNull { Badges.byId[it] }.forEach { BadgeMedallion(it, 22.dp); Spacer(Modifier.width(3.dp)) }
                Text("👣 ${user.stepsToday} · Coins $coinsToday/10", color = duo.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** One 3D-tilted cube; the centre one floats gently. */
@Composable
private fun HomeCube(tile: Tile, offset: Int, size: Dp, env: TileEnv, onClick: () -> Unit) {
    val duo = LocalDuo.current
    val shape = RoundedCornerShape(size * 0.18f)
    val (edgeA, edgeB) = tileColors(tile, duo)
    val bob = if (offset == 0) {
        val t = rememberInfiniteTransition(label = "bob")
        val v by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
        v
    } else 0f

    Box(Modifier.size(size).graphicsLayer {
        rotationY = -offset * 26f
        cameraDistance = 14f * density
        translationY = bob * density
        alpha = 1f - 0.16f * abs(offset)
    }.bounceClick(onClick = onClick)) {
        // the cube's visible side, offset away from the viewer
        val side = if (offset >= 0) 7.dp else (-7).dp
        Box(Modifier.matchParentSize().offset(x = side).clip(shape)
            .background(Brush.horizontalGradient(listOf(edgeA.copy(alpha = 0.55f), edgeB.copy(alpha = 0.9f)))))
        Box(Modifier.matchParentSize().shadow(6.dp, shape).clip(shape).background(duo.surface)
            .border(3.dp, Brush.linearGradient(listOf(edgeA, edgeB)), shape).padding(size * 0.07f)) {
            when (tile) {
                is GameTile -> Cover(tile.game, env.art[tile.game.id], Modifier.matchParentSize(), corner = size * 0.10f, showSystem = false)
                is GroupTile -> {
                    val (a, b) = GroupColors[tile.group.colorIdx.coerceIn(0, GroupColors.lastIndex)]
                    Box(Modifier.matchParentSize().clip(RoundedCornerShape(size * 0.10f))
                        .background(Brush.linearGradient(listOf(a, b))), contentAlignment = Alignment.Center) {
                        Text("📁", fontSize = (size.value * 0.4f).sp)
                    }
                }
                is SysTile -> Box(Modifier.matchParentSize().clip(RoundedCornerShape(size * 0.10f))
                    .background(Brush.linearGradient(listOf(tile.system.colorA, tile.system.colorB))), contentAlignment = Alignment.Center) {
                    Text(tile.system.short, color = Color.White, fontSize = (size.value * 0.26f).sp, fontWeight = FontWeight.ExtraBold)
                }
                is AppTile -> AppIcon(tile.app.pkg, Modifier.matchParentSize())
                is AddTile -> Box(Modifier.matchParentSize().clip(RoundedCornerShape(size * 0.10f)).background(duo.surfaceHigh),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, null, tint = duo.accent, modifier = Modifier.fillMaxSize(0.4f))
                }
            }
        }
    }
}

@Composable
private fun Hint(key: String, label: String, onClick: () -> Unit) {
    val duo = LocalDuo.current
    Row(Modifier.bounceClick(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        val round = key.length == 1
        Text(key, color = duo.muted, fontWeight = FontWeight.ExtraBold, fontSize = if (round) 12.sp else 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clip(if (round) CircleShape else RoundedCornerShape(50))
                .border(1.5.dp, duo.muted, if (round) CircleShape else RoundedCornerShape(50))
                .then(if (round) Modifier.size(22.dp).wrapContentSize() else Modifier.padding(horizontal = 7.dp, vertical = 2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, color = duo.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PlayButton(label: String, onClick: () -> Unit) {
    val duo = LocalDuo.current
    Row(Modifier.bounceClick(onClick = onClick).clip(PillShape).background(duo.accentBrush)
        .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

private fun relTime(t: Long): String {
    val m = (System.currentTimeMillis() - t) / 60_000
    return when {
        m < 2 -> "just now"
        m < 60 -> "${m} min ago"
        m < 60 * 24 -> "${m / 60} h ago"
        m < 60 * 24 * 2 -> "yesterday"
        else -> "${m / (60 * 24)} days ago"
    }
}
