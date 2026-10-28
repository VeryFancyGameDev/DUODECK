package com.duodeck.launcher.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.*
import kotlinx.coroutines.launch

/** The "touch screen": system tabs, the game grid/carousel and the action bar. */
@Composable
fun BottomScreen(vm: LibraryViewModel, onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val systems by vm.systemsPresent.collectAsStateWithLifecycle()
    val filter by vm.systemFilter.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanCount by vm.scanCount.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 10.dp)) {
            if (settings.romsUri != null) {
                // ---- system tabs ----
                LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Pill("All", filter == null, { vm.systemFilter.value = null }) }
                    rowItems(systems) { s ->
                        Pill(s.short, filter == s.id, { vm.systemFilter.value = s.id }, tint = s.colorA)
                    }
                }
                if (searching) {
                    Spacer(Modifier.height(8.dp))
                    SearchField(query, { vm.query.value = it }, Modifier.padding(horizontal = 14.dp))
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                when {
                    settings.romsUri == null -> Onboarding(onPickFolder)
                    visible.isEmpty() && scanning -> Centered("Scanning your library…\n$scanCount games found so far")
                    visible.isEmpty() -> Centered(
                        if (vm.games.value.isEmpty()) "No games found in that folder.\nTip: use subfolders named like GBA, SNES, PSP."
                        else "No games match.")
                    settings.layout == LibraryLayout.GRID -> GridView(vm, settings.gridColumns, ctx)
                    else -> CarouselView(vm, ctx)
                }
            }

            // ---- action bar ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GlossyIcon(if (settings.layout == LibraryLayout.GRID) Icons.Filled.ViewCarousel else Icons.Filled.GridView,
                    "Switch layout", { vm.toggleLayout() })
                GlossyIcon(Icons.Filled.Search, "Search", {
                    searching = !searching; if (!searching) vm.query.value = ""
                }, active = searching)
                GlossyIcon(Icons.Filled.Refresh, "Rescan library", { vm.rescan() }, active = scanning)
                GlossyIcon(Icons.Filled.Settings, "Settings", { vm.showSettings.value = true }, size = 54.dp, active = true)
            }
        }
    }
}

@Composable
private fun GridView(vm: LibraryViewModel, columns: Int, ctx: Context) {
    val visible by vm.visible.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val duo = LocalDuo.current
    val gridState = rememberLazyGridState()

    // Keep the selected tile on screen when moving with a d-pad.
    LaunchedEffect(selected?.id) {
        val idx = visible.indexOfFirst { it.id == selected?.id }
        if (idx >= 0 && gridState.layoutInfo.visibleItemsInfo.none { it.index == idx }) {
            gridState.animateScrollToItem(idx)
        }
    }

    LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize(), state = gridState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(visible, key = { it.id }) { g ->
            val isSel = g.id == selected?.id
            val s by animateFloatAsState(if (isSel) 1.06f else 1f, label = "sel")
            Box(Modifier.scale(s).bounceClick(
                onLongClick = { Launcher.launch(ctx, g, forceChooser = true) },
                onClick = { if (isSel) Launcher.launch(ctx, g) else vm.select(g) })) {
                Cover(g, art[g.id], Modifier.fillMaxWidth().aspectRatio(0.75f)
                    .then(if (isSel) Modifier.border(3.dp, duo.accent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) else Modifier))
                progress[RetroAchievements.normalise(g.title)]?.let {
                    if (it.max > 0) RaBadge(it, Modifier.align(Alignment.BottomEnd).padding(5.dp))
                }
            }
        }
    }
}

@Composable
private fun CarouselView(vm: LibraryViewModel, ctx: Context) {
    val visible by vm.visible.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val duo = LocalDuo.current
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)

    // The item resting in the middle is the selected one.
    val center by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val selectedNow by vm.selected.collectAsStateWithLifecycle()
    // Only adopt the centred item once scrolling settles, so key-driven scrolls don't fight the user.
    LaunchedEffect(center, state.isScrollInProgress, visible) {
        if (!state.isScrollInProgress) visible.getOrNull(center)?.let { vm.select(it) }
    }
    // A d-pad/gamepad move changes the selection: scroll the carousel to it.
    LaunchedEffect(selectedNow?.id) {
        val idx = visible.indexOfFirst { it.id == selectedNow?.id }
        if (idx >= 0 && idx != state.firstVisibleItemIndex && !state.isScrollInProgress) state.animateScrollToItem(idx)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val itemW = (maxHeight * 0.62f * 0.75f).coerceAtMost(maxWidth * 0.6f)
        val side = (maxWidth - itemW) / 2
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            LazyRow(state = state, flingBehavior = fling, contentPadding = PaddingValues(horizontal = side),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(visible, key = { _, g -> g.id }) { i, g ->
                    val isCenter = i == center
                    val s by animateFloatAsState(if (isCenter) 1f else 0.78f, label = "carousel")
                    val scope = rememberCoroutineScope()
                    Box(Modifier.width(itemW).scale(s).bounceClick(
                        onLongClick = { Launcher.launch(ctx, g, forceChooser = true) },
                        onClick = {
                            if (isCenter) Launcher.launch(ctx, g)
                            else scope.launch { state.animateScrollToItem(i) }
                        })) {
                        Cover(g, art[g.id], Modifier.fillMaxWidth().aspectRatio(0.75f), corner = 22.dp)
                        progress[RetroAchievements.normalise(g.title)]?.let {
                            if (it.max > 0) RaBadge(it, Modifier.align(Alignment.BottomEnd).padding(8.dp))
                        }
                    }
                }
            }
            val g = visible.getOrNull(center)
            Text(g?.title ?: "", color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp))
            Text("${center + 1} / ${visible.size}", color = duo.muted, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Onboarding(onPick: () -> Unit) {
    val duo = LocalDuo.current
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Welcome!", color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text("Point DuoDeck at your ROMs folder and your games appear. That's the only setup.",
            color = duo.muted, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        Row(Modifier.bounceClick(onClick = onPick).clip(PillShape).background(duo.accentBrush)
            .padding(horizontal = 26.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FolderOpen, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Choose ROMs folder", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Centered(text: String) {
    val duo = LocalDuo.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = duo.muted, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    BasicTextField(value, onChange, singleLine = true,
        textStyle = TextStyle(color = duo.text, fontSize = 15.sp, fontWeight = FontWeight.Medium),
        cursorBrush = SolidColor(duo.accent),
        modifier = modifier.fillMaxWidth().clip(PillShape).background(duo.surface)
            .border(1.dp, duo.stroke, PillShape).padding(horizontal = 16.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box { if (value.isEmpty()) Text("Search games…", color = duo.muted, fontSize = 15.sp); inner() }
        })
}
