package com.duodeck.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.Launcher
import com.duodeck.launcher.data.Systems

/** The "top screen": big art and details for whatever is highlighted on the bottom screen. */
@Composable
fun TopScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val game by vm.selected.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
            StatusStrip(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            AnimatedContent(
                targetState = game,
                transitionSpec = { (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth(), label = "hero",
            ) { g ->
                if (g == null) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DuoDeck", color = duo.accent, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (games.isEmpty()) "Pick your ROMs folder on the bottom screen to begin"
                        else "Nothing matches your filter",
                            color = duo.muted, fontSize = 14.sp)
                    }
                } else {
                    val sys = Systems.get(g.systemId)
                    val ra = progress[com.duodeck.launcher.data.RetroAchievements.normalise(g.title)]
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Cover(g, art[g.id], Modifier.fillMaxHeight().aspectRatio(0.72f), corner = 22.dp)
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            Text(sys?.name ?: "", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text(g.title, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                                lineHeight = 30.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Text(g.fileName, color = duo.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(14.dp))

                            if (ra != null && ra.max > 0) {
                                Text("🏆 ${ra.awarded} of ${ra.max} achievements", color = duo.text,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.height(5.dp))
                                Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(PillShape).background(duo.surfaceHigh)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(ra.awarded / ra.max.toFloat())
                                        .clip(PillShape).background(Brush.horizontalGradient(
                                            listOf(Color(0xFFFFD36B), Color(0xFFF29B1D)))))
                                }
                                Spacer(Modifier.height(14.dp))
                            }

                            val emu = sys?.let { Launcher.installed(ctx, it) }
                            Row(Modifier.bounceClick { Launcher.launch(ctx, g) }
                                .clip(PillShape).background(duo.accentBrush)
                                .padding(start = 14.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("PLAY", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            if (emu == null) {
                                Spacer(Modifier.height(6.dp))
                                Text("No ${sys?.short} emulator detected — you'll pick one when you tap Play",
                                    color = duo.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            profile?.let { p ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(p.avatarUrl, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(duo.surfaceHigh))
                    Spacer(Modifier.width(8.dp))
                    Text("${p.user} · ${p.points} pts · rank ${p.rank}", color = duo.muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
