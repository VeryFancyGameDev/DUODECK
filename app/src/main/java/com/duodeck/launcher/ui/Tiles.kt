package com.duodeck.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duodeck.launcher.AddTile
import com.duodeck.launcher.AppTile
import com.duodeck.launcher.GameTile
import com.duodeck.launcher.GroupTile
import com.duodeck.launcher.Tile
import com.duodeck.launcher.data.Game
import com.duodeck.launcher.data.RaProgress
import com.duodeck.launcher.data.RetroAchievements

/** Everything a tile needs to draw itself. */
data class TileEnv(
    val art: Map<String, String>,
    val progress: Map<String, RaProgress>,
    val favorites: Set<String>,
    val gamesById: Map<String, Game>,
    val showLabel: Boolean,
)

val TileCorner = 18.dp

/** A square icon (game, group, app or "+") with an optional name underneath, Wii U style. */
@Composable
fun TileView(
    tile: Tile,
    selected: Boolean,
    env: TileEnv,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val duo = LocalDuo.current
    val pop by animateFloatAsState(if (selected) 1.05f else 1f, label = "tilePop")
    val shape = RoundedCornerShape(TileCorner)

    Column(modifier.scale(pop).bounceClick(onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)
            .then(if (selected) Modifier.border(3.dp, duo.accent, shape) else Modifier)) {
            when (tile) {
                is GameTile -> {
                    Cover(tile.game, env.art[tile.game.id], Modifier.matchParentSize(), corner = TileCorner)
                    env.progress[RetroAchievements.normalise(tile.game.title)]?.takeIf { it.max > 0 }?.let {
                        RaBadge(it, Modifier.align(Alignment.BottomEnd).padding(5.dp))
                    }
                    if (tile.game.id in env.favorites) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD54F),
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp))
                    }
                }
                is GroupTile -> GroupSquare(tile, env)
                is AppTile -> {
                    Box(Modifier.matchParentSize().clip(shape)
                        .background(Brush.verticalGradient(listOf(duo.surfaceHigh, duo.surface)))
                        .border(1.dp, duo.stroke, shape), contentAlignment = Alignment.Center) {
                        AppIcon(tile.app.pkg, Modifier.fillMaxSize(0.62f))
                    }
                }
                is AddTile -> {
                    Box(Modifier.matchParentSize().clip(shape).background(duo.surface.copy(alpha = 0.7f))
                        .border(2.dp, duo.stroke, shape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, null, tint = duo.accent, modifier = Modifier.fillMaxSize(0.4f))
                    }
                }
            }
        }
        if (env.showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(tileLabel(tile), color = duo.text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

fun tileLabel(t: Tile): String = when (t) {
    is GameTile -> t.game.title
    is GroupTile -> t.group.name
    is AppTile -> t.app.label
    is AddTile -> if (t.kind == "group") "New group" else "Add app"
}

/** A folder tile: coloured square with a 2×2 mosaic of its first games. */
@Composable
private fun BoxScope.GroupSquare(tile: GroupTile, env: TileEnv) {
    val (a, b) = GroupColors[tile.group.colorIdx.coerceIn(0, GroupColors.lastIndex)]
    val shape = RoundedCornerShape(TileCorner)
    val games = tile.group.gameIds.mapNotNull { env.gamesById[it] }.take(4)
    Box(Modifier.matchParentSize().clip(shape).background(Brush.linearGradient(listOf(a, b)))) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))))
        if (games.isEmpty()) {
            Text("📁", fontSize = 34.sp, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.matchParentSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (col in 0..1) {
                            val g = games.getOrNull(row * 2 + col)
                            if (g != null) Cover(g, env.art[g.id], Modifier.weight(1f).fillMaxHeight(), corner = 7.dp, showSystem = false)
                            else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text("${tile.group.gameIds.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 6.dp, vertical = 1.dp))
    }
}
