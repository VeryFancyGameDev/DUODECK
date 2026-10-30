package com.duodeck.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.Dlg
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.Emulators
import com.duodeck.launcher.data.Launcher
import com.duodeck.launcher.data.LaunchMode
import com.duodeck.launcher.data.Systems

/** Pick the emulator, launch style and RetroArch core for each system. */
@Composable
fun EmulatorsScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val present by vm.systemsPresent.collectAsStateWithLifecycle()
    val systems = if (present.isEmpty()) Systems.all else present
    val installed = remember {
        (Emulators.all.filter { Launcher.isInstalled(ctx, it.pkg) } + Systems.all.flatMap { Launcher.hinted(ctx, it) }).distinctBy { it.pkg }
    }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Emulators & cores", vm)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DuoCard {
                        Text("Detected on this device", color = duo.text, fontWeight = FontWeight.Bold)
                        Text(if (installed.isEmpty()) "No supported emulators found yet. Install one (RetroArch, Dolphin, PPSSPP, Eden…) and come back."
                        else installed.joinToString(" · ") { it.label }, color = duo.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Tip: for a single game, long-press it on the home screen → “Emulator for this game”.",
                            color = duo.muted, fontSize = 11.sp)
                    }
                }
                items(systems.size) { i ->
                    val sys = systems[i]
                    val r = Launcher.resolve(ctx, sys.id, null, user)
                    val custom = user.profiles[sys.id]
                    DuoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(sys.name, color = duo.text, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (r.pkg == null) "No emulator installed"
                                    else (r.known?.label ?: r.pkg) +
                                        (if (r.pkg in Emulators.retroarchPkgs && r.core != null) " · ${r.core}" else "") +
                                        (if (r.mode == LaunchMode.AUTO || custom == null) "" else " · ${r.mode.name.lowercase()}") +
                                        (if (custom != null) "  (custom)" else ""),
                                    color = if (r.pkg == null) duo.accent else duo.muted, fontSize = 12.sp,
                                )
                            }
                            Pill("Change", true, { vm.show(Dlg.EmuProfile(sys.id, sys.id, sys.name)) })
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
