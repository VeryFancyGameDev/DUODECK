package com.duodeck.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("duodeck_settings")

data class Settings(
    val romsUris: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val themeId: String = "home",
    val layout: LibraryLayout = LibraryLayout.PAGES,
    val gridColumns: Int = 3,
    val raUser: String = "",
    val raKey: String = "",
    val sgdbKey: String = "",
    val dualScreen: Boolean = true,
    val hideNoArt: Boolean = false,
    val showLabels: Boolean = false,
    val tapToLaunch: Boolean = false,
    val musicMode: MusicMode = MusicMode.BUILTIN,
    val musicTrack: Int = 0,
    val musicVolume: Float = 0.5f,
    val musicUri: String? = null,
    val wallpaper: String? = null,
    val wallpaperDim: Float = 0.35f,
    val sort: SortMode = SortMode.NAME,
    val consoleFolders: Boolean = true,
    val tilt3d: Boolean = true,
    val steamId: String = "",
    val steamKey: String = "",
    val autoUpdateCheck: Boolean = true,
)

class Prefs(private val ctx: Context) {
    private object K {
        val roms = stringPreferencesKey("roms_uri")          // newline-separated list
        val theme = stringPreferencesKey("theme")
        val themeId = stringPreferencesKey("theme_id")
        val layout = stringPreferencesKey("layout")
        val cols = intPreferencesKey("cols")
        val raUser = stringPreferencesKey("ra_user")
        val raKey = stringPreferencesKey("ra_key")
        val sgdb = stringPreferencesKey("sgdb_key")
        val dual = booleanPreferencesKey("dual")
        val hideNoArt = booleanPreferencesKey("hide_no_art")
        val labels = booleanPreferencesKey("labels")
        val tapLaunch = booleanPreferencesKey("tap_launch")
        val musicMode = stringPreferencesKey("music_mode")
        val musicTrack = intPreferencesKey("music_track")
        val musicVol = floatPreferencesKey("music_vol")
        val musicUri = stringPreferencesKey("music_uri")
        val wallpaper = stringPreferencesKey("wallpaper")
        val wallDim = floatPreferencesKey("wall_dim")
        val sort = stringPreferencesKey("sort")
        val consoleFolders = booleanPreferencesKey("console_folders")
        val tilt3d = booleanPreferencesKey("tilt3d")
        val steamId = stringPreferencesKey("steam_id")
        val steamKey = stringPreferencesKey("steam_key")
        val autoUpd = booleanPreferencesKey("auto_update")
    }

    val flow: Flow<Settings> = ctx.store.data.map { it.toSettings() }

    private fun Preferences.toSettings() = Settings(
        romsUris = (this[K.roms] ?: "").split('\n').filter { it.isNotBlank() },
        theme = runCatching { ThemeMode.valueOf(this[K.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        themeId = this[K.themeId] ?: "home",
        layout = runCatching { LibraryLayout.valueOf(this[K.layout] ?: "PAGES") }.getOrDefault(LibraryLayout.PAGES),
        gridColumns = this[K.cols] ?: 3,
        raUser = this[K.raUser] ?: "",
        raKey = this[K.raKey] ?: "",
        sgdbKey = this[K.sgdb] ?: "",
        dualScreen = this[K.dual] ?: true,
        hideNoArt = this[K.hideNoArt] ?: false,
        showLabels = this[K.labels] ?: false,
        tapToLaunch = this[K.tapLaunch] ?: false,
        musicMode = runCatching { MusicMode.valueOf(this[K.musicMode] ?: "BUILTIN") }.getOrDefault(MusicMode.BUILTIN),
        musicTrack = this[K.musicTrack] ?: 0,
        musicVolume = this[K.musicVol] ?: 0.5f,
        musicUri = this[K.musicUri],
        wallpaper = this[K.wallpaper],
        wallpaperDim = this[K.wallDim] ?: 0.35f,
        sort = runCatching { SortMode.valueOf(this[K.sort] ?: "NAME") }.getOrDefault(SortMode.NAME),
        consoleFolders = this[K.consoleFolders] ?: true,
        tilt3d = this[K.tilt3d] ?: true,
        steamId = this[K.steamId] ?: "",
        steamKey = this[K.steamKey] ?: "",
        autoUpdateCheck = this[K.autoUpd] ?: true,
    )

    suspend fun setRoms(list: List<String>) = ctx.store.edit { it[K.roms] = list.joinToString("\n") }
    suspend fun setTheme(v: ThemeMode) = ctx.store.edit { it[K.theme] = v.name }
    suspend fun setThemeId(v: String) = ctx.store.edit { it[K.themeId] = v }
    suspend fun setLayout(v: LibraryLayout) = ctx.store.edit { it[K.layout] = v.name }
    suspend fun setCols(v: Int) = ctx.store.edit { it[K.cols] = v.coerceIn(2, 7) }
    suspend fun setRa(user: String, key: String) = ctx.store.edit { it[K.raUser] = user.trim(); it[K.raKey] = key.trim() }
    suspend fun setSgdb(key: String) = ctx.store.edit { it[K.sgdb] = key.trim() }
    suspend fun setDual(v: Boolean) = ctx.store.edit { it[K.dual] = v }
    suspend fun setHideNoArt(v: Boolean) = ctx.store.edit { it[K.hideNoArt] = v }
    suspend fun setLabels(v: Boolean) = ctx.store.edit { it[K.labels] = v }
    suspend fun setTapToLaunch(v: Boolean) = ctx.store.edit { it[K.tapLaunch] = v }
    suspend fun setMusicMode(v: MusicMode) = ctx.store.edit { it[K.musicMode] = v.name }
    suspend fun setMusicTrack(v: Int) = ctx.store.edit { it[K.musicTrack] = v }
    suspend fun setMusicVolume(v: Float) = ctx.store.edit { it[K.musicVol] = v.coerceIn(0f, 1f) }
    suspend fun setMusicUri(v: String?) = ctx.store.edit { if (v == null) it.remove(K.musicUri) else it[K.musicUri] = v }
    suspend fun setWallpaper(v: String?) = ctx.store.edit { if (v == null) it.remove(K.wallpaper) else it[K.wallpaper] = v }
    suspend fun setWallpaperDim(v: Float) = ctx.store.edit { it[K.wallDim] = v.coerceIn(0f, 0.9f) }
    suspend fun setSort(v: SortMode) = ctx.store.edit { it[K.sort] = v.name }
    suspend fun setConsoleFolders(v: Boolean) = ctx.store.edit { it[K.consoleFolders] = v }
    suspend fun setTilt3d(v: Boolean) = ctx.store.edit { it[K.tilt3d] = v }
    suspend fun setSteam(id: String, key: String) = ctx.store.edit { it[K.steamId] = id.trim(); it[K.steamKey] = key.trim() }
    suspend fun setAutoUpdate(v: Boolean) = ctx.store.edit { it[K.autoUpd] = v }
}
