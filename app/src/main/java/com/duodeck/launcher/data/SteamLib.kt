package com.duodeck.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Pulls your owned Steam games with the official Web API (needs your SteamID64 + a free API key; profile must be public). */
object SteamLib {
    suspend fun owned(steamId: String, key: String): Result<List<SteamGame>> = withContext(Dispatchers.IO) {
        runCatching {
            val u = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=$key&steamid=$steamId&include_appinfo=1&include_played_free_games=1&format=json"
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 12000; c.readTimeout = 20000
            if (c.responseCode == 403) error("Steam rejected the API key")
            if (c.responseCode != 200) error("Steam error ${c.responseCode}")
            val games = JSONObject(c.inputStream.bufferedReader().readText()).optJSONObject("response")?.optJSONArray("games")
                ?: error("No games returned. Is your Steam profile's game list public?")
            (0 until games.length()).map { games.getJSONObject(it) }
                .map { SteamGame(it.getInt("appid"), it.optString("name", "App ${it.getInt("appid")}")) }
                .sortedBy { it.name.lowercase() }
        }
    }

    fun toGame(s: SteamGame) = Game("steam:${s.appId}", s.name, "", "pc", "steam://${s.appId}")
    fun art(appId: Int) = "https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/library_600x900.jpg"
}
