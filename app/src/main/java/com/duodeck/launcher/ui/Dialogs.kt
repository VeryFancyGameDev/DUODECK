package com.duodeck.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.duodeck.launcher.*
import com.duodeck.launcher.data.*
import kotlinx.coroutines.launch

/** Draws whichever popup the view model currently wants shown. */
@Composable
fun DialogHost(vm: LibraryViewModel) {
    val d by vm.dialog.collectAsStateWithLifecycle()
    when (val dlg = d) {
        null -> Unit
        is Dlg.GameMenu -> GameMenu(vm, dlg.game)
        is Dlg.GroupMenu -> GroupMenu(vm, dlg.group)
        is Dlg.AppMenu -> AppMenu(vm, dlg.app)
        is Dlg.ArtPicker -> ArtPicker(vm, dlg.game)
        is Dlg.AddToGroup -> AddToGroup(vm, dlg.game)
        is Dlg.NewGroup -> GroupEditor("New group", "", 0, "Create", { vm.show(null) }) { n, c ->
            vm.createGroup(n, c, dlg.addGameId); vm.show(null)
        }
        is Dlg.RenameGroup -> GroupEditor("Edit group", dlg.group.name, dlg.group.colorIdx, "Save", { vm.show(null) }) { n, c ->
            vm.renameGroup(dlg.group.id, n, c); vm.show(null)
        }
        is Dlg.AddApp -> AddAppDialog(vm)
        is Dlg.EmuProfile -> EmuProfileDialog(vm, dlg.key, dlg.systemId, dlg.title)
    }
}

// ------------------------------------------------------------------ shared bits

@Composable
fun DuoField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, secret: Boolean = false) {
    val duo = LocalDuo.current
    BasicTextField(value, onChange, singleLine = true,
        textStyle = TextStyle(color = duo.text, fontSize = 15.sp),
        cursorBrush = SolidColor(duo.accent),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth().clip(PillShape).background(duo.surfaceHigh)
            .border(1.dp, duo.stroke, PillShape).padding(horizontal = 16.dp, vertical = 11.dp),
        decorationBox = { inner ->
            Box { if (value.isEmpty()) Text(hint, color = duo.muted, fontSize = 15.sp); inner() }
        })
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val duo = LocalDuo.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp))
        .background(duo.surfaceHigh).bounceClick(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = duo.accent)
        Spacer(Modifier.width(12.dp))
        Text(label, color = duo.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------------ menus

@Composable
private fun GameMenu(vm: LibraryViewModel, game: Game) {
    val user by vm.user.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val fav = game.id in user.favorites
    val close = { vm.show(null) }
    DuoDialog(game.title, close) {
        MenuRow(Icons.Filled.PlayArrow, "Play") { close(); vm.launchGame(game) }
        MenuRow(if (fav) Icons.Filled.StarBorder else Icons.Filled.Star, if (fav) "Remove favourite" else "Favourite") {
            vm.toggleFavorite(game.id); close()
        }
        MenuRow(Icons.Filled.Image, "Change box art") { vm.show(Dlg.ArtPicker(game)) }
        MenuRow(Icons.Filled.CreateNewFolder, "Add to group…") { vm.show(Dlg.AddToGroup(game)) }
        if (tab.startsWith("group:")) {
            MenuRow(Icons.Filled.RemoveCircle, "Remove from this group") {
                vm.toggleInGroup(tab.removePrefix("group:"), game.id); close()
            }
        }
        MenuRow(Icons.Filled.Tune, "Emulator for this game…") {
            vm.show(Dlg.EmuProfile("game:${game.id}", game.systemId, game.title))
        }
        MenuRow(Icons.Filled.OpenInNew, "Open with another app…") { close(); vm.launchGame(game, chooser = true) }
    }
}

@Composable
private fun GroupMenu(vm: LibraryViewModel, group: Group) {
    val close = { vm.show(null) }
    DuoDialog(group.name, close) {
        MenuRow(Icons.Filled.FolderOpen, "Open") { close(); vm.tab.value = "group:${group.id}" }
        MenuRow(Icons.Filled.Edit, "Rename / colour") { vm.show(Dlg.RenameGroup(group)) }
        MenuRow(Icons.Filled.Delete, "Delete group (games stay)") { vm.deleteGroup(group.id); close() }
    }
}

@Composable
private fun AppMenu(vm: LibraryViewModel, app: AppEntry) {
    val close = { vm.show(null) }
    DuoDialog(app.label, close) {
        MenuRow(Icons.Filled.OpenInNew, "Open") { close(); vm.activate(AppTile(app)) }
        MenuRow(Icons.Filled.Delete, "Remove from DuoDeck") { vm.removeApp(app.pkg); close() }
    }
}

// ------------------------------------------------------------------ groups

@Composable
private fun GroupEditor(
    title: String, name0: String, color0: Int, confirm: String,
    onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit,
) {
    var name by remember { mutableStateOf(name0) }
    var color by remember { mutableIntStateOf(color0) }
    DuoDialog(title, onDismiss) {
        DuoField(name, { name = it }, "Group name")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GroupColors.forEachIndexed { i, (a, b) ->
                Box(Modifier.size(34.dp).bounceClick { color = i }.clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(b, a)))
                    .border(if (color == i) 3.dp else 1.dp, if (color == i) LocalDuo.current.text else LocalDuo.current.stroke, CircleShape))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(confirm, true, { onConfirm(name, color) })
            Pill("Cancel", false, onDismiss)
        }
    }
}

@Composable
private fun AddToGroup(vm: LibraryViewModel, game: Game) {
    val user by vm.user.collectAsStateWithLifecycle()
    val duo = LocalDuo.current
    DuoDialog("Add “${game.title}” to…", { vm.show(null) }) {
        if (user.groups.isEmpty()) {
            Text("You have no groups yet.", color = duo.muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            user.groups.forEach { g ->
                val inIt = game.id in g.gameIds
                MenuRow(if (inIt) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    g.name) { vm.toggleInGroup(g.id, game.id) }
            }
        }
        MenuRow(Icons.Filled.CreateNewFolder, "New group…") { vm.show(Dlg.NewGroup(game.id)) }
        Spacer(Modifier.height(8.dp))
        Pill("Done", true, { vm.show(null) })
    }
}

// ------------------------------------------------------------------ apps

@Composable
private fun AddAppDialog(vm: LibraryViewModel) {
    val user by vm.user.collectAsStateWithLifecycle()
    val duo = LocalDuo.current
    val all by produceState(emptyList<AppEntry>()) { value = vm.launchableApps() }
    var q by remember { mutableStateOf("") }
    val shown = remember(all, q) { all.filter { q.isBlank() || it.label.contains(q, true) } }
    DuoDialog("Add apps", { vm.show(null) }) {
        DuoField(q, { q = it }, "Search apps")
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.height(330.dp)) {
            items(shown.size) { i ->
                val a = shown[i]
                val added = user.apps.any { it.pkg == a.pkg }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (added) duo.accent.copy(alpha = 0.18f) else duo.surfaceHigh)
                    .bounceClick { if (added) vm.removeApp(a.pkg) else vm.addApp(a) }
                    .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(a.pkg, Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(a.label, color = duo.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (added) Icon(Icons.Filled.CheckCircle, null, tint = duo.accent)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Pill("Done", true, { vm.show(null) })
    }
}

// ------------------------------------------------------------------ box art

@Composable
private fun ArtPicker(vm: LibraryViewModel, game: Game) {
    val duo = LocalDuo.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var term by remember { mutableStateOf(game.title) }
    var matches by remember { mutableStateOf<List<SgdbGame>>(emptyList()) }
    var images by remember { mutableStateOf<List<SgdbImage>>(emptyList()) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var status by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { vm.importArt(game.id, uri); vm.show(null) }
    }

    fun loadImages(id: Int) {
        picked = id
        scope.launch {
            status = "Loading art…"
            var list = vm.sgdb.grids(id, settings.sgdbKey, squareOnly = true)
            status = if (list.isEmpty()) {
                list = vm.sgdb.grids(id, settings.sgdbKey, squareOnly = false)
                if (list.isEmpty()) "No art found for that game." else "No square art here, showing other shapes (they'll be cropped)."
            } else ""
            images = list
        }
    }

    fun search() {
        scope.launch {
            status = "Searching…"
            matches = vm.sgdb.searchGames(term, settings.sgdbKey)
            if (matches.isEmpty()) { status = "No games found. Try a shorter name."; images = emptyList() }
            else loadImages(matches.first().id)
        }
    }

    LaunchedEffect(Unit) { if (settings.sgdbKey.isNotBlank()) search() }

    DuoDialog("Box art · ${game.title}", { vm.show(null) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Upload image", true, { pickFile.launch(arrayOf("image/*")) })
            Pill("Use automatic", false, { vm.setArt(game.id, null); vm.show(null) })
        }
        Spacer(Modifier.height(10.dp))
        if (settings.sgdbKey.isBlank()) {
            Text("Add a free SteamGridDB API key in Settings to browse artwork online. You can still upload your own image.",
                color = duo.muted, fontSize = 12.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DuoField(term, { term = it }, "Search SteamGridDB", Modifier.weight(1f))
                Pill("Go", true, { search() })
            }
            if (matches.size > 1) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(matches.size) { i ->
                        val m = matches[i]
                        Pill(m.name, picked == m.id, { loadImages(m.id) })
                    }
                }
            }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(status, color = duo.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images.size) { i ->
                    val img = images[i]
                    AsyncImage(img.thumb, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                            .background(duo.surfaceHigh)
                            .bounceClick { vm.setArt(game.id, img.url); vm.show(null) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Pill("Close", false, { vm.show(null) })
    }
}

// ------------------------------------------------------------------ emulator + core

@Composable
private fun EmuProfileDialog(vm: LibraryViewModel, key: String, systemId: String, title: String) {
    val ctx = LocalContext.current
    val duo = LocalDuo.current
    val user by vm.user.collectAsStateWithLifecycle()
    val system = Systems.get(systemId)
    val existing = user.profiles[key]

    var pkg by remember { mutableStateOf(existing?.pkg) }
    var mode by remember { mutableStateOf(existing?.mode ?: LaunchMode.AUTO) }
    var core by remember { mutableStateOf(existing?.core ?: "") }
    var showAll by remember { mutableStateOf(false) }
    val candidates = remember { system?.let { Launcher.candidates(ctx, it) }.orEmpty() }
    val allApps by produceState(emptyList<AppEntry>(), showAll) { if (showAll) value = vm.launchableApps() }

    val effectivePkg = pkg ?: candidates.firstOrNull()?.pkg
    val isRetro = mode == LaunchMode.RETROARCH || (mode == LaunchMode.AUTO && effectivePkg in Emulators.retroarchPkgs)

    DuoDialog("Emulator · $title", { vm.show(null) }) {
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            Text("EMULATOR APP", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Pill("Auto (${candidates.firstOrNull()?.label ?: "none installed"})", pkg == null, { pkg = null })
            candidates.forEach { c ->
                Spacer(Modifier.height(4.dp))
                Pill(c.label, pkg == c.pkg, { pkg = c.pkg })
            }
            Spacer(Modifier.height(4.dp))
            Pill(if (showAll) "Hide other apps" else "Other app…", false, { showAll = !showAll })
            if (showAll) {
                allApps.forEach { a ->
                    Spacer(Modifier.height(4.dp))
                    Pill(a.label, pkg == a.pkg, { pkg = a.pkg })
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("HOW TO HAND OVER THE GAME", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            Text("If a game just opens the emulator, try another style.", color = duo.muted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Auto", mode == LaunchMode.AUTO, { mode = LaunchMode.AUTO })
                Pill("File", mode == LaunchMode.VIEW_URI, { mode = LaunchMode.VIEW_URI })
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("Dolphin", mode == LaunchMode.DOLPHIN, { mode = LaunchMode.DOLPHIN })
                Pill("RetroArch", mode == LaunchMode.RETROARCH, { mode = LaunchMode.RETROARCH })
            }

            if (isRetro) {
                Spacer(Modifier.height(14.dp))
                Text("RETROARCH CORE", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                val presets = Emulators.coresFor(systemId)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets.size) { i -> Pill(presets[i], core == presets[i], { core = presets[i] }) }
                }
                Spacer(Modifier.height(6.dp))
                DuoField(core, { core = it.trim() }, "Core name, e.g. mgba")
                Text("Core must already be downloaded inside RetroArch.", color = duo.muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Save", true, {
                val none = pkg == null && mode == LaunchMode.AUTO && core.isBlank()
                vm.setProfile(key, if (none) null else LaunchProfile(pkg, core.ifBlank { null }, mode))
                vm.show(null)
            })
            Pill("Reset", false, { vm.setProfile(key, null); vm.show(null) })
        }
    }
}
