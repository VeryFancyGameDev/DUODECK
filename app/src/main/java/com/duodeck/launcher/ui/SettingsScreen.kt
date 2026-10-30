package com.duodeck.launcher.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import android.Manifest
import android.os.Build
import com.duodeck.launcher.data.DiscordFeed
import com.duodeck.launcher.data.Steps
import com.duodeck.launcher.data.Updater
import com.duodeck.launcher.data.SortMode
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.Screen
import com.duodeck.launcher.data.LibraryLayout
import com.duodeck.launcher.data.MusicMode
import com.duodeck.launcher.data.MusicSynth
import com.duodeck.launcher.data.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ScreenHeader(title: String, vm: LibraryViewModel) {
    val duo = LocalDuo.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GlossyIcon(Icons.Filled.ArrowBack, "Back", { vm.back() }, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(title, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
    }
}

@Composable
fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val duo = LocalDuo.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = duo.text, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, color = duo.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked, onChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = duo.accent))
    }
}

@Composable
fun SettingsScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val raStatus by vm.raStatus.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val theme by vm.activeTheme.collectAsStateWithLifecycle()
    val scope = vm.viewModelScope

    val user by vm.user.collectAsStateWithLifecycle()
    val steamStatus by vm.steamStatus.collectAsStateWithLifecycle()
    val upd by vm.update.collectAsStateWithLifecycle()
    val updStatus by vm.updateStatus.collectAsStateWithLifecycle()
    var steamId by remember(s.steamId) { mutableStateOf(s.steamId) }
    var steamKey by remember(s.steamKey) { mutableStateOf(s.steamKey) }
    val stepPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.syncSteps() }
    val stepOk = Build.VERSION.SDK_INT < 29 ||
        androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    var raUser by remember(s.raUser) { mutableStateOf(s.raUser) }
    var raKey by remember(s.raKey) { mutableStateOf(s.raKey) }
    var sgdb by remember(s.sgdbKey) { mutableStateOf(s.sgdbKey) }
    var vol by remember(s.musicVolume) { mutableFloatStateOf(s.musicVolume) }
    var dim by remember(s.wallpaperDim) { mutableFloatStateOf(s.wallpaperDim) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addRomFolder(uri)
    }
    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importWallpaper(uri)
    }
    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { vm.prefs.setMusicUri(uri.toString()); vm.prefs.setMusicMode(MusicMode.CUSTOM) }
        }
    }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Settings", vm)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {

                SectionLabel("Library")
                DuoCard {
                    Text("${games.size} games found", color = duo.text, fontWeight = FontWeight.Bold)
                    Text("Games are grouped by subfolder name (GBA, SNES, Switch, PSP…) or file type.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    s.romsUris.forEach { u ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(folderName(u), color = duo.text, fontSize = 13.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Pill("Remove", false, { vm.removeRomFolder(u) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(if (s.romsUris.isEmpty()) "Choose folder" else "Add another folder", true, { pickFolder.launch(null) })
                        Pill("Rescan", false, { vm.rescan() })
                    }
                }

                SectionLabel("Home screen")
                DuoCard {
                    Text("Layout", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Pages", s.layout == LibraryLayout.PAGES, { scope.launch { vm.prefs.setLayout(LibraryLayout.PAGES) } })
                        Pill("Grid", s.layout == LibraryLayout.GRID, { scope.launch { vm.prefs.setLayout(LibraryLayout.GRID) } })
                        Pill("Carousel", s.layout == LibraryLayout.CAROUSEL, { scope.launch { vm.prefs.setLayout(LibraryLayout.CAROUSEL) } })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Tiles per row: ${s.gridColumns}", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("−", false, { scope.launch { vm.prefs.setCols(s.gridColumns - 1) } })
                        Pill("+", false, { scope.launch { vm.prefs.setCols(s.gridColumns + 1) } })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Sort games", color = duo.text, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("A–Z", s.sort == SortMode.NAME, { scope.launch { vm.prefs.setSort(SortMode.NAME) } })
                        Pill("Recent", s.sort == SortMode.RECENT, { scope.launch { vm.prefs.setSort(SortMode.RECENT) } })
                        Pill("Most played", s.sort == SortMode.MOST_PLAYED, { scope.launch { vm.prefs.setSort(SortMode.MOST_PLAYED) } })
                    }
                    ToggleRow("Console folders", "Adds a Consoles tab that groups your games by system, like iiSU.", s.consoleFolders) {
                        scope.launch { vm.prefs.setConsoleFolders(it) }
                    }
                    ToggleRow("3D tilt tiles", "Tiles have real thickness and shift as you tilt your phone.", s.tilt3d) {
                        scope.launch { vm.prefs.setTilt3d(it) }
                    }
                    ToggleRow("Show names under tiles", "", s.showLabels) { scope.launch { vm.prefs.setLabels(it) } }
                    ToggleRow("Tap once to launch", "Off: first tap selects and previews, second tap launches.", s.tapToLaunch) {
                        scope.launch { vm.prefs.setTapToLaunch(it) }
                    }
                    ToggleRow("Use second display when available",
                        "On dual-screen devices the top screen goes on the second display.", s.dualScreen) {
                        scope.launch { vm.prefs.setDual(it) }
                    }
                }

                SectionLabel("Appearance")
                DuoCard {
                    Text("Brightness", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.values().forEach { m ->
                            Pill(m.name.lowercase().replaceFirstChar { it.uppercase() }, s.theme == m,
                                { scope.launch { vm.prefs.setTheme(m) } })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Theme: ${theme.name}", color = duo.text, fontWeight = FontWeight.Bold)
                    Text("by ${theme.author}", color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Pill("Open Theme Store", true, { vm.go(Screen.THEMES) })

                    Spacer(Modifier.height(14.dp))
                    Text("Wallpaper", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Choose image", true, { pickWallpaper.launch(arrayOf("image/*")) })
                        if (s.wallpaper != null) Pill("Remove", false, { scope.launch { vm.prefs.setWallpaper(null) } })
                    }
                    if (s.wallpaper != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Dim: ${(dim * 100).toInt()}%", color = duo.muted, fontSize = 12.sp)
                        Slider(dim, { dim = it }, valueRange = 0f..0.9f,
                            onValueChangeFinished = { scope.launch { vm.prefs.setWallpaperDim(dim) } },
                            colors = SliderDefaults.colors(thumbColor = duo.accent, activeTrackColor = duo.accent))
                    }
                }

                SectionLabel("Home menu music")
                DuoCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Off", s.musicMode == MusicMode.OFF, { scope.launch { vm.prefs.setMusicMode(MusicMode.OFF) } })
                        Pill("Built-in", s.musicMode == MusicMode.BUILTIN, { scope.launch { vm.prefs.setMusicMode(MusicMode.BUILTIN) } })
                        Pill("My file", s.musicMode == MusicMode.CUSTOM, {
                            if (s.musicUri == null) pickMusic.launch(arrayOf("audio/*"))
                            else scope.launch { vm.prefs.setMusicMode(MusicMode.CUSTOM) }
                        })
                    }
                    Spacer(Modifier.height(8.dp))
                    when (s.musicMode) {
                        MusicMode.BUILTIN -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MusicSynth.names.forEachIndexed { i, n ->
                                    Pill(n, s.musicTrack == i, { scope.launch { vm.prefs.setMusicTrack(i) } })
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(MusicSynth.blurbs[s.musicTrack.coerceIn(0, MusicSynth.blurbs.lastIndex)],
                                color = duo.muted, fontSize = 12.sp)
                            Text("Original loops made on your device. They play only while DuoDeck is open.",
                                color = duo.muted, fontSize = 11.sp)
                        }
                        MusicMode.CUSTOM -> {
                            Text(if (s.musicUri == null) "No file chosen" else folderName(s.musicUri!!),
                                color = duo.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            Pill("Choose audio file", false, { pickMusic.launch(arrayOf("audio/*")) })
                            Text("Any song you have as a file (mp3, ogg, m4a, wav…). It loops on the home screen.",
                                color = duo.muted, fontSize = 11.sp)
                        }
                        MusicMode.OFF -> Unit
                    }
                    if (s.musicMode != MusicMode.OFF) {
                        Spacer(Modifier.height(8.dp))
                        Text("Volume: ${(vol * 100).toInt()}%", color = duo.muted, fontSize = 12.sp)
                        Slider(vol, { vol = it }, valueRange = 0f..1f,
                            onValueChangeFinished = { scope.launch { vm.prefs.setMusicVolume(vol) } },
                            colors = SliderDefaults.colors(thumbColor = duo.accent, activeTrackColor = duo.accent))
                    }
                }

                SectionLabel("You & emulators")
                DuoCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Avatar maker", true, { vm.go(Screen.AVATAR) })
                        Pill("Emulators & cores", true, { vm.go(Screen.EMULATORS) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Badge Arcade", false, { vm.go(Screen.ARCADE) })
                        Pill("Discord", false, { vm.go(Screen.DISCORD) })
                    }
                }

                SectionLabel("Play Coins from steps")
                DuoCard {
                    Text("Walk with your phone and earn Play Coins: 100 steps = 1 coin, up to ${Steps.DAILY_CAP} a day.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    if (!Steps.available(ctx)) {
                        Text("This device has no step counter sensor.", color = duo.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Today: ${user.stepsToday} steps · ${user.stepCoinsToday}/${Steps.DAILY_CAP} coins", color = duo.text, fontWeight = FontWeight.Bold)
                        Text("Total: ${user.totalSteps} steps", color = duo.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        if (!stepOk) Pill("Allow step counting", true, { stepPerm.launch(Manifest.permission.ACTIVITY_RECOGNITION) })
                        else Pill("Count my steps now", false, { vm.syncSteps() })
                        Text("Steps are added up whenever you open DuoDeck, even if you walked while it was closed.",
                            color = duo.muted, fontSize = 11.sp)
                    }
                }

                SectionLabel("Steam library")
                DuoCard {
                    Text("Shows your Steam games as PC tiles. Games open in GameNative (straight into the game) or in GameHub / Winlator / Droid Deck (opens the app). Needs your SteamID64 and a free Web API key from steamcommunity.com/dev/apikey, and your game list must be public.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    DuoField(steamId, { steamId = it }, "SteamID64 (17 digits)")
                    Spacer(Modifier.height(8.dp))
                    DuoField(steamKey, { steamKey = it }, "Steam Web API key", secret = true)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Save & sync", true, { scope.launch { vm.prefs.setSteam(steamId, steamKey); vm.syncSteam() } })
                        if (user.steam.isNotEmpty()) Pill("Remove", false, { vm.clearSteam() })
                    }
                    val msg = steamStatus.ifBlank { if (user.steam.isNotEmpty()) "${user.steam.size} Steam games" else "" }
                    if (msg.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(msg, color = duo.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                SectionLabel("Updates")
                DuoCard {
                    Text("Installed: ${Updater.installedName(ctx)} (build ${Updater.installedBuild(ctx)})", color = duo.text, fontWeight = FontWeight.Bold)
                    Text("DuoDeck updates itself from GitHub, so you never need to download the file again.", color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Check for updates", upd == null, { vm.checkUpdate(true) })
                        if (upd != null) Pill("Update now", true, { vm.installUpdate() })
                    }
                    if (updStatus.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(updStatus, color = duo.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    ToggleRow("Check automatically", "Looks for a new version each time you open DuoDeck.", s.autoUpdateCheck) {
                        scope.launch { vm.prefs.setAutoUpdate(it) }
                    }
                }

                SectionLabel("RetroAchievements")
                DuoCard {
                    Text("Shows your points, rank and per-game progress. Get your Web API key at retroachievements.org → Settings → Keys.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    DuoField(raUser, { raUser = it }, "Username")
                    Spacer(Modifier.height(8.dp))
                    DuoField(raKey, { raKey = it }, "Web API key", secret = true)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Connect", true, { scope.launch { vm.prefs.setRa(raUser, raKey) }; vm.refreshRa() })
                        if (s.raUser.isNotBlank()) Pill("Disconnect", false, {
                            raUser = ""; raKey = ""; scope.launch { vm.prefs.setRa("", "") }
                        })
                    }
                    if (raStatus.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(raStatus, color = duo.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                SectionLabel("SteamGridDB")
                DuoCard {
                    Text("Downloads square cover art automatically and lets you browse options per game. Free API key: steamgriddb.com → Preferences → API.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    DuoField(sgdb, { sgdb = it }, "API key", secret = true)
                    Spacer(Modifier.height(10.dp))
                    Pill("Save key", true, { scope.launch { vm.prefs.setSgdb(sgdb) } })
                    Spacer(Modifier.height(10.dp))
                    ToggleRow("Hide games without art", "Only show games that have a cover.", s.hideNoArt) {
                        scope.launch { vm.prefs.setHideNoArt(it) }
                    }
                }

                SectionLabel("About")
                DuoCard {
                    Text("DuoDeck ${Updater.installedName(ctx)}", color = duo.text, fontWeight = FontWeight.ExtraBold)
                    Text("To use DuoDeck as your home screen, choose it under Android Settings → Apps → Default apps → Home app.",
                        color = duo.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

private fun folderName(uri: String): String {
    val d = Uri.decode(uri)
    return d.substringAfterLast(':').substringAfterLast('/').ifEmpty { d }
}
