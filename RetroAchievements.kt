package com.duodeck.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal RetroAchievements Web API client (https://api-docs.retroachievements.org). */
object RetroAchievements {
    private const val BASE = "https://retroachievements.org"

    suspend fun profile(user: String, key: String): RaProfile? = withContext(Dispatchers.IO) {
        if (user.isBlank() || key.isBlank()) return@withContext null
        runCatching {
            val o = JSONObject(call("API_GetUserSummary.php", user, key, "&g=5&a=0"))
            val recent = o.optJSONArray("RecentlyPlayed")
            RaProfile(
                user = o.optString("User", user),
                points = o.optInt("TotalPoints"),
                softcore = o.optInt("TotalSoftcorePoints"),
                rank = o.opt("Rank")?.toString() ?: "-",
                avatarUrl = o.optString("UserPic").takeIf { it.isNotBlank() }?.let { "$BASE$it" },
                recent = (0 until (recent?.length() ?: 0)).map { i ->
                    recent!!.getJSONObject(i).optString("Title")
                },
            )
        }.getOrNull()
    }

    /** Per-game achievement progress, keyed by normalised title for matching against ROM names. */
    suspend fun progress(user: String, key: String): Map<String, RaProgress> = withContext(Dispatchers.IO) {
        if (user.isBlank() || key.isBlank()) return@withContext emptyMap()
        runCatching {
            val o = JSONObject(call("API_GetUserCompletionProgress.php", user, key, "&c=500"))
            val arr = o.optJSONArray("Results") ?: return@runCatching emptyMap()
            (0 until arr.length()).associate { i ->
                val g = arr.getJSONObject(i)
                val title = g.optString("Title")
                normalise(title) to RaProgress(
                    title = title,
                    awarded = g.optInt("NumAwarded"),
                    max = g.optInt("MaxPossible"),
                    icon = g.optString("ImageIcon").takeIf { it.isNotBlank() }?.let { "$BASE$it" },
                )
            }
        }.getOrDefault(emptyMap())
    }

    fun normalise(title: String): String =
        title.lowercase().replace(Regex("[^a-z0-9]"), "")

    /** Quick credential check used by the Settings screen. */
    suspend fun verify(user: String, key: String): Boolean = profile(user, key) != null

    private fun call(endpoint: String, user: String, key: String, extra: String): String {
        val u = URLEncoder.encode(user, "UTF-8")
        val k = URLEncoder.encode(key, "UTF-8")
        val c = URL("$BASE/API/$endpoint?z=$u&y=$k&u=$u$extra").openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 10000
        return try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }
}
