package com.duodeck.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel

/** A small floating "update available" card, shown when a newer build exists on GitHub. */
@Composable
fun UpdateBanner(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val info by vm.update.collectAsStateWithLifecycle()
    val status by vm.updateStatus.collectAsStateWithLifecycle()
    val progress by vm.updateProgress.collectAsStateWithLifecycle()
    var hidden by remember { mutableStateOf(false) }
    val u = info
    if (u == null || hidden) return
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.systemBarsPadding().padding(top = 6.dp, start = 16.dp, end = 16.dp).shadow(8.dp, shape)
        .clip(shape).background(duo.surface).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f, fill = false)) {
                Text("Update available", color = duo.text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(if (progress >= 0f) "Downloading ${(progress * 100).toInt()}%" else status.ifBlank { u.name },
                    color = duo.muted, fontSize = 11.sp, maxLines = 2)
            }
            Pill("Update", true, { vm.installUpdate() })
            Pill("Later", false, { hidden = true })
        }
        if (progress >= 0f) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(PillShape).background(duo.surfaceHigh)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(PillShape).background(duo.accent))
            }
        }
    }
}
