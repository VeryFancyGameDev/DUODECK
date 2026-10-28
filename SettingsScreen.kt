package com.duodeck.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.LibraryLayout
import com.duodeck.launcher.data.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: LibraryViewModel, onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val raStatus by vm.raStatus.collectAsStateWithLifecycle()
    val games by vm.games.collectAsStateWithLifecycle()
    val scope = vm.viewModelScope

    var raUser by remember(s.raUser) { mutableStateOf(s.raUser) }
    var raKey by remember(s.raKey) { mutableStateOf(s.raKey) }
    var sgdb by remember(s.sgdbKey) { mutableStateOf(s.sgdbKey) }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // header
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlossyIcon(Icons.Filled.ArrowBack, "Back", { vm.showSettings.value = false }, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Text("Settings", color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {

                SectionLabel("Library")
                Card {
                    Text(if (s.romsUri == null) "No folder chosen" else "${games.size} games found",
                        color = duo.text, fontWeight = FontWeight.Bold)
                    Text("Games are grouped by subfolder name (GBA, SNES, PSP…) or file type.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(if (s.romsUri == null) "Choose folder" else "Change folder", true, onPickFolder)
                        Pill("Rescan", false, { vm.rescan() })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Grid columns: ${s.gridColumns}", color = duo.text, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("−", false, { scope.launch { vm.prefs.setCols(s.gridColumns - 1) } })
                        Pill("+", false, { scope.launch { vm.prefs.setCols(s.gridColumns + 1) } })
                    }
                }

                SectionLabel("Appearance")
                Card {
                    Text("Theme", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.values().forEach { m ->
                            Pill(m.name.lowercase().replaceFirstChar { it.uppercase() }, s.theme == m,
                                { scope.launch { vm.prefs.setTheme(m) } })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Accent colour", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accents.forEachIndexed { i, (_, cols) ->
                            Box(Modifier.size(36.dp).bounceClick { scope.launch { vm.prefs.setAccent(i) } }
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(cols.second, cols.first)))
                                .border(if (s.accent == i) 3.dp else 1.dp,
                                    if (s.accent == i) duo.text else duo.stroke, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Game browser", color = duo.text, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Grid", s.layout == LibraryLayout.GRID, { scope.launch { vm.prefs.setLayout(LibraryLayout.GRID) } })
                        Pill("Carousel", s.layout == LibraryLayout.CAROUSEL, { scope.launch { vm.prefs.setLayout(LibraryLayout.CAROUSEL) } })
                    }
                }

                SectionLabel("Dual screen")
                Card {
                    ToggleRow("Use second display when available",
                        "On devices with two screens, the top screen shows on the second display. Otherwise both screens share this one.",
                        s.dualScreen) { scope.launch { vm.prefs.setDual(it) } }
                }

                SectionLabel("RetroAchievements")
                Card {
                    Text("Shows your points, rank and per-game progress. Get your Web API key at retroachievements.org → Settings → Keys.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Field(raUser, { raUser = it }, "Username")
                    Spacer(Modifier.height(8.dp))
                    Field(raKey, { raKey = it }, "Web API key", secret = true)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Card {
                    Text("Downloads cover art automatically. Free API key: steamgriddb.com → Preferences → API. Without one, games get colourful placeholder tiles.",
                        color = duo.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Field(sgdb, { sgdb = it }, "API key", secret = true)
                    Spacer(Modifier.height(10.dp))
                    Pill("Save key", true, { scope.launch { vm.prefs.setSgdb(sgdb) } })
                    Spacer(Modifier.height(10.dp))
                    ToggleRow("Hide games without art", "Only show games that have a cover.", s.hideNoArt) {
                        scope.launch { vm.prefs.setHideNoArt(it) }
                    }
                }

                SectionLabel("About")
                Card {
                    Text("DuoDeck 1.0", color = duo.text, fontWeight = FontWeight.ExtraBold)
                    Text("To use DuoDeck as your home screen, choose it under Android Settings → Apps → Default apps → Home app.",
                        color = duo.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val duo = LocalDuo.current
    Column(Modifier.fillMaxWidth().clip(CardShape).background(duo.surface)
        .border(1.dp, duo.stroke, CardShape).padding(16.dp), content = content)
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val duo = LocalDuo.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = duo.text, fontWeight = FontWeight.Bold)
            Text(sub, color = duo.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked, onChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = duo.accent))
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String, secret: Boolean = false) {
    val duo = LocalDuo.current
    BasicTextField(value, onChange, singleLine = true,
        textStyle = TextStyle(color = duo.text, fontSize = 15.sp),
        cursorBrush = SolidColor(duo.accent),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().clip(PillShape).background(duo.surfaceHigh)
            .border(1.dp, duo.stroke, PillShape).padding(horizontal = 16.dp, vertical = 11.dp),
        decorationBox = { inner ->
            Box { if (value.isEmpty()) Text(hint, color = duo.muted, fontSize = 15.sp); inner() }
        })
}
