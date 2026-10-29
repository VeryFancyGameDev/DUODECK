package com.duodeck.launcher.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.ThemeDef
import com.duodeck.launcher.data.toJson

/** 3DS-style theme shop: browse online themes, install, apply, import/share your own. */
@Composable
fun ThemeStoreScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val store by vm.storeThemes.collectAsStateWithLifecycle()
    val status by vm.storeStatus.collectAsStateWithLifecycle()
    val mine by vm.allThemes.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.fetchStore() }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() }.getOrNull().orEmpty()
            val n = vm.importThemes(text)
            Toast.makeText(ctx, if (n > 0) "Imported $n theme${if (n == 1) "" else "s"}" else "That file isn't a DuoDeck theme", Toast.LENGTH_SHORT).show()
            if (n > 0) tab = 1
        }
    }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Theme Store", vm)
            Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Store", tab == 0, { tab = 0 })
                Pill("My themes", tab == 1, { tab = 1 })
                Pill("Import", false, { importer.launch(arrayOf("*/*")) })
            }
            Spacer(Modifier.height(8.dp))

            val list: List<ThemeDef> = if (tab == 0) store else mine
            val installedIds = mine.map { it.id }.toSet()

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tab == 0 && status.isNotEmpty()) {
                    item { Text(status, color = duo.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                if (tab == 0) {
                    item {
                        Text("New themes appear here when they're added online. Anyone can make one: share a theme file and use Import.",
                            color = duo.muted, fontSize = 12.sp)
                    }
                }
                items(list.size) { i ->
                    val t = list[i]
                    val installed = t.id in installedIds
                    val active = s.themeId == t.id
                    ThemeCard(
                        t, active, installed,
                        onGet = { vm.installTheme(t); vm.applyTheme(t.id) },
                        onApply = { vm.applyTheme(t.id) },
                        onRemove = if (installed && mine.firstOrNull { it.id == t.id }?.builtin == false) ({ vm.removeTheme(t.id) }) else null,
                        onShare = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, "DuoDeck theme: ${t.name}")
                                .putExtra(Intent.EXTRA_TEXT, t.toJson().toString(2))
                            ctx.startActivity(Intent.createChooser(send, "Share theme").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    t: ThemeDef, active: Boolean, installed: Boolean,
    onGet: () -> Unit, onApply: () -> Unit, onRemove: (() -> Unit)?, onShare: () -> Unit,
) {
    val duo = LocalDuo.current
    DuoCard {
        ThemePreview(t, Modifier.fillMaxWidth().height(110.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.name, color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("by ${t.author}", color = duo.muted, fontSize = 12.sp)
            }
            if (active) Text("IN USE", color = duo.accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
        if (t.blurb.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(t.blurb, color = duo.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                active -> Pill("Applied", false, {})
                installed -> Pill("Apply", true, onApply)
                else -> Pill("Get", true, onGet)
            }
            Pill("Share", false, onShare)
            if (onRemove != null) Pill("Remove", false, onRemove)
        }
    }
}

/** A little mock home screen painted with the theme's colours (follows light/dark). */
@Composable
fun ThemePreview(t: ThemeDef, modifier: Modifier = Modifier) {
    val dark = LocalDuo.current.dark
    val p = if (dark) t.dark else t.light
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(p.bgTop), Color(p.bgBottom))))
        .border(1.dp, Color(p.stroke), shape).padding(12.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Color(p.accent2), Color(p.accent)))))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(50)).background(Color(p.text)))
                Box(Modifier.fillMaxWidth(0.55f).height(8.dp).clip(RoundedCornerShape(50)).background(Color(p.muted)))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Color(p.surface))
                            .border(1.dp, Color(p.stroke), RoundedCornerShape(7.dp)))
                    }
                    Box(Modifier.width(44.dp).height(22.dp).clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(p.accent2), Color(p.accent)))))
                }
            }
        }
    }
}
