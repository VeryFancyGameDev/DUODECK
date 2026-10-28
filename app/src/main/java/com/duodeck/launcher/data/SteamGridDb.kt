package com.duodeck.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** SteamGridDB cover lookup. Results are cached on disk so each game is looked up once. */
class SteamGridDb(ctx: Context) {
    private val file = File(ctx.filesDir, "art_cache.json")
    private val cache = JSONObject(runCatching { file.readText() }.getOrDefault("{}"))

    fun cached(game: Game): String? =
        cache.optString(game.id).takeIf { it.isNotEmpty() && it != "none" }

    fun hasLookedUp(game: Game) = cache.has(game.id)

    suspend fun fetchCover(game: Game, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        cached(game)?.let { return@withContext it }
        runCatching {
            val term = URLEncoder.encode(game.title, "UTF-8").replace("+", "%20")
            val search = get("https://www.steamgriddb.com/api/v2/search/autocomplete/$term", apiKey)
            val gameId = JSONObject(search).optJSONArray("data")?.optJSONObject(0)?.optInt("id", -1) ?: -1
            var url: String? = null
            if (gameId > 0) {
                val grids = get(
                    "https://www.steamgriddb.com/api/v2/grids/game/$gameId?dimensions=600x900,342x482&types=static&nsfw=false",
                    apiKey,
                )
                url = JSONObject(grids).optJSONArray("data")?.optJSONObject(0)?.optString("url")
            }
            cache.put(game.id, url ?: "none")
            file.writeText(cache.toString())
            url
        }.getOrNull()
    }

    /** Forget "no result" entries so a newly added key retries them. */
    fun clearMisses() {
        cache.keys().asSequence().toList().forEach { if (cache.optString(it) == "none") cache.remove(it) }
        file.writeText(cache.toString())
    }

    private fun get(url: String, key: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $key")
        c.connectTimeout = 8000
        c.readTimeout = 8000
        return try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().readText()
        } finally {
            c.disconnect()
        }
    }
}
