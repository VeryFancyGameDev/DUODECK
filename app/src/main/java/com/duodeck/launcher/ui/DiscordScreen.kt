package com.duodeck.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duodeck.launcher.LibraryViewModel
import com.duodeck.launcher.data.DiscordFeed
import com.duodeck.launcher.data.Launcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Your Discord messages, read from the Discord app's notifications. */
@Composable
fun DiscordScreen(vm: LibraryViewModel, modifier: Modifier = Modifier) {
    val duo = LocalDuo.current
    val ctx = LocalContext.current
    val msgs by DiscordFeed.msgs.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(DiscordFeed.hasAccess(ctx)) }
    val owner = LocalLifecycleOwner.current

    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) access = DiscordFeed.hasAccess(ctx) }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    LaunchedEffect(Unit) { DiscordFeed.markRead() }
    LaunchedEffect(msgs.size) { DiscordFeed.markRead() }

    DotBackground(modifier) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            ScreenHeader("Discord", vm)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DuoCard {
                        if (!access) {
                            Text("Turn on message access", color = duo.text, fontWeight = FontWeight.ExtraBold)
                            Text("DuoDeck shows your Discord messages by reading the Discord app's notifications. Nothing is sent anywhere and you don't log in. Only Discord's notifications are used.",
                                color = duo.muted, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Pill("Open notification access", true, {
                                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            })
                            Spacer(Modifier.height(6.dp))
                            Text("If DuoDeck is greyed out there: Android Settings → Apps → DuoDeck → ⋮ menu → Allow restricted settings, then try again.",
                                color = duo.muted, fontSize = 11.sp)
                        } else {
                            Text("Connected", color = duo.text, fontWeight = FontWeight.ExtraBold)
                            Text("New Discord notifications appear below and on the top screen. Keep Discord's own notifications on, and don't swipe them away before they show up here. Only messages that arrive while this is on are kept.",
                                color = duo.muted, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("Open Discord", true, { Launcher.launchApp(ctx, DiscordFeed.PKG) })
                                if (msgs.isNotEmpty()) Pill("Clear", false, { DiscordFeed.clear(ctx) })
                            }
                        }
                    }
                }
                if (access && msgs.isEmpty()) {
                    item { Text("No messages yet. They'll show up here as they arrive.", color = duo.muted, fontSize = 13.sp) }
                }
                items(msgs) { m ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(duo.surface).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.author, color = Color(0xFF5865F2), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                                modifier = Modifier.weight(1f, fill = false))
                            if (m.where.isNotBlank() && m.where != m.author) {
                                Text("  in ${m.where}", color = duo.muted, fontSize = 11.sp, maxLines = 1)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(m.time)), color = duo.muted, fontSize = 11.sp)
                        }
                        Text(m.text, color = duo.text, fontSize = 14.sp)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
