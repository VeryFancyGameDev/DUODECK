package com.duodeck.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import com.duodeck.launcher.SysTile
import com.duodeck.launcher.data.Systems
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val CardCorner = 20.dp
private val ArtCorner = 12.dp

/**
 * A white rounded card holding a square icon (game, group, app or "+"), like the handheld home menu.
 * The selected one gets a pulsing mint corner frame.
 */
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
    val shape = RoundedCornerShape(CardCorner)
    val tilt = LocalTilt.current
    val density = LocalDensity.current.density
    val (edgeA, edgeB) = tileColors(tile, duo)
    val pulse: Float = if (selected) {
        val t = rememberInfiniteTransition(label = "reticle")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
        v
    } else 0f
    val depth by androidx.compose.animation.core.animateFloatAsState(if (selected) 1.35f else 1f, label = "depth")

    Column(modifier.bounceClick(onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .graphicsLayer {
                    val t = tilt.value
                    rotationY = t.x * 9f
                    rotationX = -t.y * 9f
                    cameraDistance = 14f * density
                }
                .drawBehind { drawExtrusion(edgeA, edgeB, tilt.value, depth, CardCorner.toPx()) }
                .drawBehind { if (selected) drawReticle(duo.accent, pulse) }
                .shadow(4.dp, shape, clip = false)
                .clip(shape)
                .background(duo.surface)
                .border(3.dp, Brush.linearGradient(listOf(edgeA, edgeB)), shape)
                .padding(9.dp),
        ) {
            when (tile) {
                is GameTile -> {
                    Cover(tile.game, env.art[tile.game.id], Modifier.matchParentSize(), corner = ArtCorner, showSystem = false)
                    env.progress[RetroAchievements.normalise(tile.game.title)]?.takeIf { it.max > 0 }?.let {
                        RaBadge(it, Modifier.align(Alignment.BottomEnd).padding(4.dp))
                    }
                    if (tile.game.id in env.favorites) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp))
                    }
                }
                is GroupTile -> GroupSquare(tile, env)
                is SysTile -> ConsoleSquare(tile, env)
                is AppTile -> AppIcon(tile.app.pkg, Modifier.matchParentSize())
                is AddTile -> Box(Modifier.matchParentSize().clip(RoundedCornerShape(ArtCorner)).background(duo.surfaceHigh),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, null, tint = duo.accent, modifier = Modifier.fillMaxSize(0.42f))
                }
            }
        }
        if (env.showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(tileLabel(tile), color = duo.text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Outline colours: the console's own colours for games and console folders, the group colour for groups. */
internal fun tileColors(tile: Tile, duo: Duo): Pair<Color, Color> = when (tile) {
    is GameTile -> Systems.get(tile.game.systemId)?.let { it.colorA to it.colorB } ?: (duo.accent to duo.accent2)
    is SysTile -> tile.system.colorA to tile.system.colorB
    is GroupTile -> GroupColors[tile.group.colorIdx.coerceIn(0, GroupColors.lastIndex)]
    is AppTile -> duo.accent to duo.accent2
    is AddTile -> duo.stroke to duo.stroke
}

/**
 * A slab behind the face: stacked copies shifted by the phone tilt, getting darker toward the back,
 * so the tile has real thickness that parallaxes as you tilt the phone.
 */
private fun DrawScope.drawExtrusion(a: Color, b: Color, tilt: Offset, depth: Float, corner: Float) {
    val layers = 9
    val dx = (4.dp.toPx() - tilt.x * 11.dp.toPx()) * depth
    val dy = (6.dp.toPx() + tilt.y * 11.dp.toPx()) * depth
    val r = CornerRadius(corner)
    val sideTop = lerp(a, Color.Black, 0.25f)
    val sideBottom = lerp(a, Color.Black, 0.62f)
    // soft contact shadow under the whole slab
    drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(dx * 1.15f + 2.dp.toPx(), dy * 1.15f + 4.dp.toPx()), size, r)
    for (i in layers downTo 1) {
        val f = i / layers.toFloat()
        drawRoundRect(lerp(sideTop, sideBottom, f), Offset(dx * f, dy * f), size, r)
    }
}

/** Four mint corner brackets around the selected tile, with a soft glow. */
private fun DrawScope.drawReticle(color: Color, pulse: Float) {
    val out = (6.dp + (pulse * 3f).dp).toPx()
    val len = 26.dp.toPx()
    val sw = 5.dp.toPx()
    val l = -out
    val t = -out
    val r = size.width + out
    val b = size.height + out
    fun corner(x: Float, y: Float, dx: Float, dy: Float): Path = Path().apply {
        moveTo(x, y + dy * len); lineTo(x, y); lineTo(x + dx * len, y)
    }
    val corners = listOf(corner(l, t, 1f, 1f), corner(r, t, -1f, 1f), corner(l, b, 1f, -1f), corner(r, b, -1f, -1f))
    corners.forEach { drawPath(it, color.copy(alpha = 0.30f), style = Stroke(sw * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    corners.forEach { drawPath(it, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
}

fun tileLabel(t: Tile): String = when (t) {
    is GameTile -> t.game.title
    is GroupTile -> t.group.name
    is AppTile -> t.app.label
    is AddTile -> if (t.kind == "group") "New group" else "Add app"
    is SysTile -> t.system.name
}

/** A folder tile: coloured square with a 2×2 mosaic of its first games. */
@Composable
private fun BoxScope.GroupSquare(tile: GroupTile, env: TileEnv) {
    val (a, b) = GroupColors[tile.group.colorIdx.coerceIn(0, GroupColors.lastIndex)]
    val shape = RoundedCornerShape(ArtCorner)
    val games = tile.group.gameIds.mapNotNull { env.gamesById[it] }.take(4)
    Box(Modifier.matchParentSize().clip(shape).background(Brush.linearGradient(listOf(a, b)))) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))))
        if (games.isEmpty()) {
            Text("📁", fontSize = 30.sp, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.matchParentSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0..1) {
                            val g = games.getOrNull(row * 2 + col)
                            if (g != null) Cover(g, env.art[g.id], Modifier.weight(1f).fillMaxHeight(), corner = 6.dp, showSystem = false)
                            else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text("${tile.group.gameIds.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 6.dp, vertical = 1.dp))
    }
}

/** A console folder: the console's gradient, a mosaic of its first games, and its short name + count. */
@Composable
private fun BoxScope.ConsoleSquare(tile: SysTile, env: TileEnv) {
    val shape = RoundedCornerShape(ArtCorner)
    val withArt = tile.sample.filter { env.art[it.id] != null }.take(4)
    Box(Modifier.matchParentSize().clip(shape)
        .background(Brush.linearGradient(listOf(tile.system.colorA, tile.system.colorB)))) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))))
        if (withArt.isNotEmpty()) {
            Column(Modifier.matchParentSize().padding(7.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0..1) {
                            val g = withArt.getOrNull(row * 2 + col)
                            if (g != null) Cover(g, env.art[g.id], Modifier.weight(1f).fillMaxHeight(), corner = 6.dp, showSystem = false)
                            else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Text(tile.system.short, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center))
        }
        Text(tile.system.short, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 6.dp, vertical = 1.dp))
        Text("${tile.count}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 6.dp, vertical = 1.dp))
    }
}
