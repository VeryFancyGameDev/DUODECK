package com.duodeck.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.*
import com.duodeck.launcher.data.LibraryLayout
import kotlinx.coroutines.launch

/** The "touch screen": a dock of shortcuts above a cream panel of square tile cards. */
@Composable
fun BottomScreen(vm: LibraryViewModel, onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val systems by vm.systemsPresent.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanCount by vm.scanCount.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val art by vm.art.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }
    var showSystems by remember { mutableStateOf(false) }

    val byId = remember(games) { games.associateBy { it.id } }
    val env = TileEnv(art, progress, user.favorites, byId, settings.showLabels)

    val onTile: (Tile) -> Unit = { t ->
        if (settings.tapToLaunch || t is AddTile || t is GroupTile || t.key == selected?.key) vm.activate(t) else vm.select(t)
    }
    val onLong: (Tile) -> Unit = { vm.longPress(it) }

    val gameTab = tab == "all" || tab == "fav" || tab == "recent" || tab.startsWith("sys:") || tab.startsWith("group:")
    val needsRoms = games.isEmpty() && settings.romsUris.isEmpty() && gameTab
    val inGroup = tab.startsWith("group:")

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Dock(
                tab = tab, layout = settings.layout, user = user,
                onTab = { t ->
                    if (t == "all" && (tab == "all" || tab.startsWith("sys:"))) showSystems = !showSystems
                    vm.tab.value = t
                },
                onSettings = { vm.go(Screen.SETTINGS) },
                onLayout = { vm.cycleLayout() },
                onAvatar = { vm.go(Screen.AVATAR) },
            )

            if (inGroup) {
                val g = user.groups.firstOrNull { "group:${it.id}" == tab }
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("‹ Groups", false, { vm.tab.value = "groups" })
                    Pill(g?.name ?: "Group", true, {}, tint = GroupColors[(g?.colorIdx ?: 0).coerceIn(0, GroupColors.lastIndex)].first)
                }
            } else if (showSystems || tab.startsWith("sys:")) {
                LazyRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Pill("All", tab == "all", { vm.tab.value = "all" }) }
                    items(systems.size) { i ->
                        val s = systems[i]
                        Pill(s.short, tab == "sys:${s.id}", { vm.tab.value = "sys:${s.id}" }, tint = s.colorA)
                    }
                }
            } else Spacer(Modifier.height(6.dp))

            if (searching) {
                SearchField(query, { vm.query.value = it }, Modifier.padding(horizontal = 14.dp).padding(bottom = 6.dp))
            }

            // ---- the cream panel ----
            val panelShape = RoundedCornerShape(26.dp)
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)
                .clip(panelShape).background(LocalDuo.current.surfaceHigh)
                .border(1.dp, LocalDuo.current.stroke, panelShape)) {
                when {
                    needsRoms -> Onboarding(onPickFolder)
                    tiles.isEmpty() && scanning -> Centered("Scanning your library…\n$scanCount games found so far")
                    tiles.isEmpty() -> Centered(emptyMessage(tab, games.isEmpty()))
                    settings.layout == LibraryLayout.PAGES ->
                        PagesView(tiles, selected, env, settings.gridColumns, onTile, onLong)
                    settings.layout == LibraryLayout.GRID ->
                        GridView(tiles, selected, env, settings.gridColumns, onTile, onLong)
                    else -> CarouselView(vm, tiles, selected, env, onTile, onLong)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.07f), Color.Transparent))))
            }

            // ---- small action bar ----
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GlossyIcon(Icons.Filled.Search, "Search", {
                    searching = !searching; if (!searching) vm.query.value = ""
                }, active = searching, size = 40.dp)
                GlossyIcon(if (tab == "apps") Icons.Filled.Add else Icons.Filled.CreateNewFolder, "Add", {
                    vm.show(if (tab == "apps") Dlg.AddApp else Dlg.NewGroup())
                }, size = 40.dp)
                GlossyIcon(Icons.Filled.Casino, "Random game", { vm.randomGame() }, size = 40.dp)
                GlossyIcon(Icons.Filled.Refresh, "Rescan library", { vm.rescan() }, active = scanning, size = 40.dp)
            }
        }
    }
}

// ---------------------------------------------------------------- dock

@Composable
private fun Dock(
    tab: String, layout: LibraryLayout, user: com.duodeck.launcher.data.UserState,
    onTab: (String) -> Unit, onSettings: () -> Unit, onLayout: () -> Unit, onAvatar: () -> Unit,
) {
    val duo = LocalDuo.current
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        // settings tab on the left edge
        Box(Modifier.width(58.dp).fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
            .background(duo.surface.copy(alpha = 0.92f)).bounceClick(onClick = onSettings),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Settings, "Settings", tint = duo.accent, modifier = Modifier.size(30.dp))
        }
        val items = listOf(
            Triple("all", Icons.Filled.SportsEsports, Color(0xFFFF8F00)),
            Triple("fav", Icons.Filled.Star, Color(0xFFFFB300)),
            Triple("recent", Icons.Filled.History, Color(0xFF00BFA5)),
            Triple("groups", Icons.Filled.Folder, Color(0xFF43C32F)),
            Triple("apps", Icons.Filled.Apps, Color(0xFF2979FF)),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (key, icon, tint) ->
                val on = tab == key || (key == "all" && tab.startsWith("sys:")) || (key == "groups" && tab.startsWith("group:"))
                DockIcon(icon, tint, on) { onTab(key) }
            }
        }
        // layout + avatar capsule
        Row(Modifier.padding(end = 8.dp).clip(RoundedCornerShape(18.dp)).background(duo.surface.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(when (layout) {
                LibraryLayout.PAGES -> Icons.Filled.ViewModule
                LibraryLayout.GRID -> Icons.Filled.GridView
                LibraryLayout.CAROUSEL -> Icons.Filled.ViewCarousel
            }, "Layout", tint = Color(0xFF5C7CFA),
                modifier = Modifier.size(34.dp).bounceClick(onClick = onLayout).padding(3.dp))
            AvatarView(user.avatar, Modifier.size(34.dp).clip(CircleShape).bounceClick(onClick = onAvatar))
        }
    }
}

@Composable
private fun DockIcon(icon: ImageVector, tint: Color, on: Boolean, onClick: () -> Unit) {
    val duo = LocalDuo.current
    val lift by animateFloatAsState(if (on) 1.12f else 1f, label = "dock")
    Box(Modifier.size(44.dp).scale(lift).bounceClick(onClick = onClick).clip(RoundedCornerShape(14.dp))
        .background(if (on) duo.surface else Color.Transparent), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
    }
}

private fun emptyMessage(tab: String, noGames: Boolean): String = when {
    tab == "fav" -> "No favourites yet.\nLong-press a game and choose Favourite."
    tab == "recent" -> "Games you play will show up here."
    tab.startsWith("group:") -> "This group is empty.\nLong-press a game and choose Add to group."
    noGames -> "No games found in your folder.\nTip: use subfolders named like GBA, SNES, Switch, PSP."
    else -> "Nothing matches."
}

// ---------------------------------------------------------------- layouts

@Composable
private fun PagesView(
    tiles: List<Tile>, selected: Tile?, env: TileEnv, columns: Int,
    onTile: (Tile) -> Unit, onLong: (Tile) -> Unit,
) {
    val duo = LocalDuo.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val gap = 16.dp
        val pad = 20.dp
        val tileW: Dp = (maxWidth - pad * 2 - gap * (columns - 1)) / columns
        val labelH = if (env.showLabel) 22.dp else 0.dp
        val dotsH = 24.dp
        val rows = (((maxHeight - dotsH - 12.dp) + gap) / (tileW + labelH + gap)).toInt().coerceAtLeast(1)
        val per = rows * columns
        val pages = remember(tiles, per) { tiles.chunked(per) }
        val pager: PagerState = rememberPagerState(pageCount = { pages.size })
        val scope = rememberCoroutineScope()

        LaunchedEffect(selected?.key, per) {
            val idx = tiles.indexOfFirst { it.key == selected?.key }
            if (idx >= 0 && pager.currentPage != idx / per) pager.animateScrollToPage(idx / per)
        }

        Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                    Column(Modifier.fillMaxSize().padding(horizontal = pad), verticalArrangement = Arrangement.spacedBy(gap)) {
                        for (r in 0 until rows) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                for (c in 0 until columns) {
                                    val t = pages[page].getOrNull(r * columns + c)
                                    if (t != null) {
                                        TileView(t, t.key == selected?.key, env, Modifier.weight(1f),
                                            onClick = { onTile(t) }, onLongClick = { onLong(t) })
                                    } else Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                if (pager.currentPage < pages.size - 1) {
                    PageArrow(Icons.Filled.ArrowForward, Modifier.align(Alignment.CenterEnd).offset(x = 16.dp)) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                }
                if (pager.currentPage > 0) {
                    PageArrow(Icons.Filled.ArrowBack, Modifier.align(Alignment.CenterStart).offset(x = (-16).dp)) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    }
                }
            }
            if (pages.size > 1) {
                Row(Modifier.fillMaxWidth().height(dotsH), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    for (i in pages.indices) {
                        val on = i == pager.currentPage
                        Box(Modifier.padding(horizontal = 4.dp).size(if (on) 9.dp else 7.dp).clip(CircleShape)
                            .background(if (on) duo.accent else duo.muted.copy(alpha = 0.4f))
                            .bounceClick { scope.launch { pager.animateScrollToPage(i) } })
                    }
                }
            } else Spacer(Modifier.height(dotsH))
        }
    }
}

@Composable
private fun PageArrow(icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val duo = LocalDuo.current
    Box(modifier.size(54.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(duo.surface)
        .bounceClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = duo.accent, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun GridView(
    tiles: List<Tile>, selected: Tile?, env: TileEnv, columns: Int,
    onTile: (Tile) -> Unit, onLong: (Tile) -> Unit,
) {
    val state = rememberLazyGridState()
    LaunchedEffect(selected?.key) {
        val idx = tiles.indexOfFirst { it.key == selected?.key }
        if (idx >= 0 && state.layoutInfo.visibleItemsInfo.none { it.index == idx }) state.animateScrollToItem(idx)
    }
    LazyVerticalGrid(GridCells.Fixed(columns), Modifier.fillMaxSize(), state = state,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(tiles.size, key = { tiles[it].key }) { i ->
            val t = tiles[i]
            TileView(t, t.key == selected?.key, env, onClick = { onTile(t) }, onLongClick = { onLong(t) })
        }
    }
}

@Composable
private fun CarouselView(
    vm: LibraryViewModel, tiles: List<Tile>, selected: Tile?, env: TileEnv,
    onTile: (Tile) -> Unit, onLong: (Tile) -> Unit,
) {
    val duo = LocalDuo.current
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)
    val center by remember { derivedStateOf { state.firstVisibleItemIndex } }

    LaunchedEffect(center, state.isScrollInProgress, tiles) {
        if (!state.isScrollInProgress) tiles.getOrNull(center)?.let { vm.select(it) }
    }
    LaunchedEffect(selected?.key) {
        val idx = tiles.indexOfFirst { it.key == selected?.key }
        if (idx >= 0 && idx != state.firstVisibleItemIndex && !state.isScrollInProgress) state.animateScrollToItem(idx)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val itemW = (maxHeight * 0.62f).coerceAtMost(maxWidth * 0.6f)
        val side = (maxWidth - itemW) / 2
        val scope = rememberCoroutineScope()
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            LazyRow(state = state, flingBehavior = fling, contentPadding = PaddingValues(horizontal = side),
                horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().weight(1f)) {
                itemsIndexed(tiles, key = { _, t -> t.key }) { i, t ->
                    val isCenter = i == center
                    val s by animateFloatAsState(if (isCenter) 1f else 0.78f, label = "carousel")
                    Box(Modifier.width(itemW).padding(8.dp).scale(s)) {
                        TileView(t, isCenter, env.copy(showLabel = false), Modifier.fillMaxWidth(),
                            onClick = { if (isCenter) onTile(t) else scope.launch { state.animateScrollToItem(i) } },
                            onLongClick = { onLong(t) })
                    }
                }
            }
            Text(tiles.getOrNull(center)?.let { tileLabel(it) } ?: "", color = duo.text,
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp))
            Text("${center + 1} / ${tiles.size}", color = duo.muted, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        }
    }
}

// ---------------------------------------------------------------- misc

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
            Box { if (value.isEmpty()) Text("Search…", color = duo.muted, fontSize = 15.sp); inner() }
        })
}
