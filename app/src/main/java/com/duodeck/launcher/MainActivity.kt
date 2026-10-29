package com.duodeck.launcher

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.duodeck.launcher.data.LibraryLayout
import com.duodeck.launcher.data.MusicManager
import com.duodeck.launcher.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by viewModels()
    private var presentation: SecondScreen? = null
    private var hasSecondDisplay by mutableStateOf(false)
    private lateinit var music: MusicManager

    private val displays by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) = syncSecondScreen()
        override fun onDisplayRemoved(id: Int) = syncSecondScreen()
        override fun onDisplayChanged(id: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        music = MusicManager(applicationContext)

        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            val theme by vm.activeTheme.collectAsStateWithLifecycle()
            val screen by vm.screen.collectAsStateWithLifecycle()
            val tab by vm.tab.collectAsStateWithLifecycle()
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) vm.addRomFolder(uri)
            }

            BackHandler(enabled = screen != Screen.HOME || tab.startsWith("group:")) {
                if (screen != Screen.HOME) vm.back() else vm.tab.value = "groups"
            }

            DuoTheme(s.theme, theme, s.wallpaper, s.wallpaperDim) {
                val external = hasSecondDisplay && s.dualScreen

                @Composable
                fun Bottom(m: Modifier) {
                    when (screen) {
                        Screen.HOME -> BottomScreen(vm, { picker.launch(null) }, m)
                        Screen.SETTINGS -> SettingsScreen(vm, m)
                        Screen.THEMES -> ThemeStoreScreen(vm, m)
                        Screen.AVATAR -> AvatarMakerScreen(vm, m)
                        Screen.EMULATORS -> EmulatorsScreen(vm, m)
                    }
                }

                if (external || screen != Screen.HOME) {
                    Bottom(Modifier.fillMaxSize())
                } else {
                    // One physical screen: stack the two "screens" (or put them side by side in landscape).
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        if (maxWidth > maxHeight) {
                            Row(Modifier.fillMaxSize()) {
                                TopScreen(vm, Modifier.weight(1f).fillMaxHeight())
                                Bottom(Modifier.weight(1f).fillMaxHeight())
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                TopScreen(vm, Modifier.weight(0.40f).fillMaxWidth())
                                Bottom(Modifier.weight(0.60f).fillMaxWidth())
                            }
                        }
                    }
                }
                DialogHost(vm)
            }
        }

        displays.registerDisplayListener(displayListener, null)
        lifecycleScope.launch { vm.settings.collect { syncSecondScreen(); music.configure(it) } }
    }

    override fun onStart() { super.onStart(); syncSecondScreen(); music.resume() }
    override fun onStop() { music.pause(); dismissSecondScreen(); super.onStop() }
    override fun onDestroy() {
        displays.unregisterDisplayListener(displayListener)
        music.release()
        super.onDestroy()
    }

    // ---------- second physical display ----------

    private fun syncSecondScreen() {
        val target = displays.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        hasSecondDisplay = target != null
        if (target == null || !vm.settings.value.dualScreen) { dismissSecondScreen(); return }
        if (presentation?.display?.displayId == target.displayId && presentation?.isShowing == true) return
        dismissSecondScreen()
        runCatching {
            presentation = SecondScreen(this, target) {
                val s by vm.settings.collectAsStateWithLifecycle()
                val theme by vm.activeTheme.collectAsStateWithLifecycle()
                DuoTheme(s.theme, theme, s.wallpaper, s.wallpaperDim) { TopScreen(vm, Modifier.fillMaxSize()) }
            }.also { it.show() }
        }
    }

    private fun dismissSecondScreen() {
        presentation?.dismiss()
        presentation = null
    }

    // ---------- gamepad / d-pad ----------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (vm.screen.value != Screen.HOME || vm.dialog.value != null) return super.onKeyDown(keyCode, event)
        val tiles = vm.tiles.value
        val settings = vm.settings.value
        val cols = if (settings.layout == LibraryLayout.CAROUSEL) 1 else settings.gridColumns
        val cur = tiles.indexOfFirst { it.key == vm.selected.value?.key }.coerceAtLeast(0)
        fun go(i: Int) { if (tiles.isNotEmpty()) vm.select(tiles[i.coerceIn(0, tiles.lastIndex)]) }
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { go(cur - 1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { go(cur + 1); true }
            KeyEvent.KEYCODE_DPAD_UP -> { go(cur - cols); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { go(cur + cols); true }
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                tiles.getOrNull(cur)?.let { vm.activate(it) }; true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (vm.tab.value.startsWith("group:")) vm.tab.value = "groups"; true
            }
            KeyEvent.KEYCODE_BUTTON_X -> { tiles.getOrNull(cur)?.let { vm.longPress(it) }; true }
            KeyEvent.KEYCODE_BUTTON_Y -> { vm.cycleLayout(); true }
            KeyEvent.KEYCODE_BUTTON_START -> { vm.go(Screen.SETTINGS); true }
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                val order = listOf("all", "fav", "recent", "groups", "apps") + vm.systemsPresent.value.map { "sys:${it.id}" }
                val i = order.indexOf(vm.tab.value).coerceAtLeast(0)
                val step = if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) 1 else -1
                vm.tab.value = order[(i + step + order.size) % order.size]
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

/** Hosts the top screen on a second physical display using Compose. */
class SecondScreen(
    private val host: ComponentActivity,
    display: Display,
    private val content: @Composable () -> Unit,
) : Presentation(host, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.decorView?.let {
            it.setViewTreeLifecycleOwner(host)
            it.setViewTreeViewModelStoreOwner(host)
            it.setViewTreeSavedStateRegistryOwner(host)
        }
        setContentView(ComposeView(context).apply { setContent(content) })
    }
}
