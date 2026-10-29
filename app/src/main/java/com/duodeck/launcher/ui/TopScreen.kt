package com.duodeck.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.duodeck.launcher.*
import com.duodeck.launcher.data.Launcher
import com.duodeck.launcher.data.RetroAchievements
import com.duodeck.launcher.data.Systems

/** The "top screen": big art and details for whatever is highlighted on the bottom screen. */
@Composable
fun TopScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val tile by vm.selected.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val byId = remember(games) { games.associateBy { it.id } }
    val recents = user.recents.mapNotNull { byId[it] }.take(8)

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusStrip(Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Row(Modifier.bounceClick { vm.go(Screen.AVATAR) }.clip(PillShape).background(duo.surface)
                    .border(1.dp, duo.stroke, PillShape).padding(start = 3.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AvatarView(user.avatar, Modifier.size(28.dp).clip(CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(user.avatar.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState = tile,
                transitionSpec = { (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(), label = "hero",
            ) { t ->
                when (t) {
                    null, is AddTile -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DuoDeck", color = duo.accent, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (games.isEmpty()) "Pick your ROMs folder on the bottom screen to begin"
                        else "Pick something to play", color = duo.muted, fontSize = 14.sp)
                    }
                    is GameTile -> {
                        val g = t.game
                        val sys = Systems.get(g.systemId)
                        val ra = progress[RetroAchievements.normalise(g.title)]
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Cover(g, art[g.id], Modifier.fillMaxHeight().aspectRatio(1f), corner = 22.dp)
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                                Text(sys?.name ?: "", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text(g.title, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                                    lineHeight = 28.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Text(g.fileName, color = duo.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(12.dp))
                                if (ra != null && ra.max > 0) {
                                    Text("🏆 ${ra.awarded} of ${ra.max} achievements", color = duo.text,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.height(5.dp))
                                    Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(PillShape).background(duo.surfaceHigh)) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(ra.awarded / ra.max.toFloat())
                                            .clip(PillShape).background(Brush.horizontalGradient(
                                                listOf(Color(0xFFFFD36B), Color(0xFFF29B1D)))))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                                PlayButton("PLAY") { vm.launchGame(g) }
                                val r = Launcher.resolve(ctx, g.systemId, g.id, user)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (r.pkg == null) "No ${sys?.short ?: ""} emulator found. You'll pick one when you tap Play."
                                    else "Plays in ${r.known?.label ?: r.pkg}" +
                                        (if (r.pkg in com.duodeck.launcher.data.Emulators.retroarchPkgs && r.core != null) " · ${r.core}" else ""),
                                    color = duo.muted, fontSize = 11.sp, maxLines = 2)
                            }
                        }
                    }
                    is GroupTile -> {
                        val gs = t.group.gameIds.mapNotNull { byId[it] }
                        val (c1, c2) = GroupColors[t.group.colorIdx.coerceIn(0, GroupColors.lastIndex)]
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(c1, c2))), contentAlignment = Alignment.Center) {
                                Text("📁", fontSize = 64.sp)
                            }
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                                Text("GROUP", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Text(t.group.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, maxLines = 2)
                                Text("${gs.size} game${if (gs.size == 1) "" else "s"}", color = duo.muted, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                                PlayButton("OPEN") { vm.activate(t) }
                            }
                        }
                    }
                    is AppTile -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(22.dp))
                            .background(duo.surface).border(1.dp, duo.stroke, RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center) {
                            AppIcon(t.app.pkg, Modifier.fillMaxSize(0.62f))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            Text("APP", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text(t.app.label, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, maxLines = 2)
                            Spacer(Modifier.height(12.dp))
                            PlayButton("OPEN") { vm.activate(t) }
                        }
                    }
                }
            }

            if (recents.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text("RECENTLY PLAYED", color = duo.muted, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recents.size) { i ->
                        val g = recents[i]
                        Cover(g, art[g.id], Modifier.size(44.dp).bounceClick { vm.jumpToGame(g) }, corner = 10.dp, showSystem = false)
                    }
                }
            }

            profile?.let { p ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(p.avatarUrl, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(22.dp).clip(CircleShape).background(duo.surfaceHigh))
                    Spacer(Modifier.width(8.dp))
                    Text("${p.user} · ${p.points} pts · rank ${p.rank}", color = duo.muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlayButton(label: String, onClick: () -> Unit) {
    val duo = LocalDuo.current
    Row(Modifier.bounceClick(onClick = onClick).clip(PillShape).background(duo.accentBrush)
        .padding(start = 14.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}
