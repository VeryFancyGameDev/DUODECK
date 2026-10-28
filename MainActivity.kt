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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.duodeck.launcher.data.Launcher
import com.duodeck.launcher.data.LibraryLayout
import com.duodeck.launcher.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by viewModels()
    private var presentation: SecondScreen? = null
    private var hasSecondDisplay by mutableStateOf(false)

    private val displays by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) = syncSecondScreen()
        override fun onDisplayRemoved(id: Int) = syncSecondScreen()
        override fun onDisplayChanged(id: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) vm.pickRomFolder(uri)
            }
            val settingsOpen by vm.showSettings.collectAsStateWithLifecycle()
            BackHandler(enabled = settingsOpen) { vm.showSettings.value = false }

            DuoTheme(s.theme, s.accent) {
                val onPick = { picker.launch(null) }
                val external = hasSecondDisplay && s.dualScreen

                @Composable
                fun Bottom(m: Modifier) {
                    if (settingsOpen) SettingsScreen(vm, onPick, m) else BottomScreen(vm, onPick, m)
                }

                if (external) {
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
                                TopScreen(vm, Modifier.weight(0.42f).fillMaxWidth())
                                Bottom(Modifier.weight(0.58f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        displays.registerDisplayListener(displayListener, null)
        lifecycleScope.launch { vm.settings.collect { syncSecondScreen() } }
    }

    override fun onStart() { super.onStart(); syncSecondScreen() }
    override fun onStop() { dismissSecondScreen(); super.onStop() }
    override fun onDestroy() { displays.unregisterDisplayListener(displayListener); super.onDestroy() }

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
                DuoTheme(s.theme, s.accent) { TopScreen(vm, Modifier.fillMaxSize()) }
            }.also { it.show() }
        }
    }

    private fun dismissSecondScreen() {
        presentation?.dismiss()
        presentation = null
    }

    // ---------- gamepad / d-pad ----------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val list = vm.visible.value
        if (list.isEmpty()) return super.onKeyDown(keyCode, event)
        val cols = if (vm.settings.value.layout == LibraryLayout.GRID) vm.settings.value.gridColumns else 1
        val cur = list.indexOfFirst { it.id == vm.selected.value?.id }.coerceAtLeast(0)
        fun go(i: Int) { vm.select(list[i.coerceIn(0, list.lastIndex)]) }
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { go(cur - 1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { go(cur + 1); true }
            KeyEvent.KEYCODE_DPAD_UP -> { go(cur - cols); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { go(cur + cols); true }
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                Launcher.launch(this, list[cur]); true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> { vm.toggleLayout(); true }
            KeyEvent.KEYCODE_BUTTON_START -> { vm.showSettings.value = !vm.showSettings.value; true }
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                val sys = vm.systemsPresent.value
                if (sys.isNotEmpty()) {
                    val order = listOf<String?>(null) + sys.map { it.id }
                    val i = order.indexOf(vm.systemFilter.value)
                    val step = if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) 1 else -1
                    vm.systemFilter.value = order[(i + step + order.size) % order.size]
                }
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
