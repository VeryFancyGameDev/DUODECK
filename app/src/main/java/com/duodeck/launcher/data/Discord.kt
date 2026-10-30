package com.duodeck.launcher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DiscordMsg(val author: String, val text: String, val where: String, val time: Long)

/**
 * Discord messages, collected from the Discord app's own notifications (no token, no login, nothing leaves
 * the phone). You need to grant DuoDeck "Notification access" once; only com.discord notifications are read.
 */
object DiscordFeed {
    const val PKG = "com.discord"
    private val _msgs = MutableStateFlow<List<DiscordMsg>>(emptyList())
    val msgs: StateFlow<List<DiscordMsg>> = _msgs
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread
    private var loaded = false

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    private fun file(ctx: Context) = File(ctx.filesDir, "discord.json")

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val a = JSONArray(file(ctx).readText())
            _msgs.value = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                DiscordMsg(o.optString("a"), o.optString("t"), o.optString("w"), o.optLong("ts"))
            }
        }
    }

    private fun save(ctx: Context) {
        runCatching {
            file(ctx).writeText(JSONArray().apply {
                _msgs.value.forEach { put(JSONObject().put("a", it.author).put("t", it.text).put("w", it.where).put("ts", it.time)) }
            }.toString())
        }
    }

    fun markRead() { _unread.value = 0 }

    fun clear(ctx: Context) { _msgs.value = emptyList(); _unread.value = 0; save(ctx) }

    internal fun ingest(ctx: Context, sbn: StatusBarNotification) {
        if (sbn.packageName != PKG) return
        load(ctx)
        val n = sbn.notification ?: return
        // Skip the grouped "3 new messages" summary, the individual ones carry the real text.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras ?: return
        val found = ArrayList<DiscordMsg>()
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val sub = ex.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val now = sbn.postTime

        @Suppress("DEPRECATION")
        val styled = ex.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (styled != null && styled.isNotEmpty()) {
            for (p in styled) {
                val b = p as? Bundle ?: continue
                val text = b.getCharSequence("text")?.toString().orEmpty()
                val who = b.getCharSequence("sender")?.toString()
                    ?: (b.getBundle("sender_person")?.getCharSequence("name")?.toString())
                    ?: title
                if (text.isNotBlank()) found += DiscordMsg(who, text, title.ifBlank { sub }, b.getLong("time", now))
            }
        } else {
            val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            if (text.isNotBlank()) found += DiscordMsg(title, text, sub, now)
        }
        if (found.isEmpty()) return
        val known = _msgs.value.map { Triple(it.author, it.text, it.where) }.toSet()
        val fresh = found.filter { Triple(it.author, it.text, it.where) !in known }
        if (fresh.isEmpty()) return
        _msgs.value = (fresh.reversed() + _msgs.value).take(120)
        _unread.value = _unread.value + fresh.size
        save(ctx)
    }
}

class DiscordListener : NotificationListenerService() {
    override fun onListenerConnected() {
        runCatching { activeNotifications?.forEach { DiscordFeed.ingest(applicationContext, it) } }
        DiscordFeed.markRead()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        DiscordFeed.ingest(applicationContext, sbn)
    }

    companion object {
        fun component(ctx: Context) = ComponentName(ctx, DiscordListener::class.java)
    }
}
