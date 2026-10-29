package com.duodeck.launcher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SgdbGame(val id: Int, val name: String)
data class SgdbImage(val url: String, val thumb: String, val style: String)

/** SteamGridDB lookup. Automatic picks are cached on disk so each game is looked up once. */
class SteamGridDb(ctx: Context) {
    private val file = File(ctx.filesDir, "art_cache.json")
    private val cache = JSONObject(runCatching { file.readText() }.getOrDefault("{}"))

    fun cached(game: Game): String? =
        cache.optString(game.id).takeIf { it.isNotEmpty() && it != "none" }

    fun hasLookedUp(game: Game) = cache.has(game.id)

    /** Picks the best square cover for a game automatically (falls back to any aspect ratio). */
    suspend fun fetchCover(game: Game, apiKey: String): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        cached(game)?.let { return@withContext it }
        runCatching {
            val gameId = searchGames(game.title, apiKey).firstOrNull()?.id
            var url: String? = null
            if (gameId != null) {
                url = grids(gameId, apiKey, squareOnly = true).firstOrNull()?.url
                    ?: grids(gameId, apiKey, squareOnly = false).firstOrNull()?.url
            }
            cache.put(game.id, url ?: "none")
            file.writeText(cache.toString())
            url
        }.getOrNull()
    }

    suspend fun searchGames(term: String, apiKey: String): List<SgdbGame> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || term.isBlank()) return@withContext emptyList()
        runCatching {
            val t = URLEncoder.encode(term, "UTF-8").replace("+", "%20")
            val arr = JSONObject(get("https://www.steamgriddb.com/api/v2/search/autocomplete/$t", apiKey)).optJSONArray("data")
            (0 until (arr?.length() ?: 0)).map {
                val o = arr!!.getJSONObject(it)
                SgdbGame(o.optInt("id"), o.optString("name"))
            }.filter { it.id > 0 }.take(8)
        }.getOrDefault(emptyList())
    }

    /** All grid images for a game. Square ones (512/1024) are ideal for DuoDeck's square tiles. */
    suspend fun grids(gameId: Int, apiKey: String, squareOnly: Boolean): List<SgdbImage> = withContext(Dispatchers.IO) {
        runCatching {
            val dims = if (squareOnly) "&dimensions=512x512,1024x1024" else ""
            val body = get("https://www.steamgriddb.com/api/v2/grids/game/$gameId?types=static&nsfw=false$dims", apiKey)
            val arr = JSONObject(body).optJSONArray("data")
            (0 until (arr?.length() ?: 0)).map {
                val o = arr!!.getJSONObject(it)
                val url = o.optString("url")
                SgdbImage(url, o.optString("thumb").ifEmpty { url }, o.optString("style"))
            }.filter { it.url.isNotEmpty() }.take(45)
        }.getOrDefault(emptyList())
    }

    /** Forget "no result" entries so a newly added key retries them. */
    fun clearMisses() {
        cache.keys().asSequence().toList().forEach { if (cache.optString(it) == "none") cache.remove(it) }
        runCatching { file.writeText(cache.toString()) }
    }

    private fun get(url: String, key: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $key")
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
