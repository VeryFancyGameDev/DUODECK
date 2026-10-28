package com.duodeck.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("duodeck_settings")

data class Settings(
    val romsUri: String? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val accent: Int = 0,
    val gridColumns: Int = 4,
    val raUser: String = "",
    val raKey: String = "",
    val sgdbKey: String = "",
    val dualScreen: Boolean = true,
    val hideNoArt: Boolean = false,
)

class Prefs(private val ctx: Context) {
    private object K {
        val roms = stringPreferencesKey("roms_uri")
        val theme = stringPreferencesKey("theme")
        val layout = stringPreferencesKey("layout")
        val accent = intPreferencesKey("accent")
        val cols = intPreferencesKey("cols")
        val raUser = stringPreferencesKey("ra_user")
        val raKey = stringPreferencesKey("ra_key")
        val sgdb = stringPreferencesKey("sgdb_key")
        val dual = booleanPreferencesKey("dual")
        val hideNoArt = booleanPreferencesKey("hide_no_art")
    }

    val flow: Flow<Settings> = ctx.store.data.map { p -> p.toSettings() }

    private fun Preferences.toSettings() = Settings(
        romsUri = this[K.roms],
        theme = runCatching { ThemeMode.valueOf(this[K.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        layout = runCatching { LibraryLayout.valueOf(this[K.layout] ?: "GRID") }.getOrDefault(LibraryLayout.GRID),
        accent = this[K.accent] ?: 0,
        gridColumns = this[K.cols] ?: 4,
        raUser = this[K.raUser] ?: "",
        raKey = this[K.raKey] ?: "",
        sgdbKey = this[K.sgdb] ?: "",
        dualScreen = this[K.dual] ?: true,
        hideNoArt = this[K.hideNoArt] ?: false,
    )

    suspend fun setRoms(uri: String?) = ctx.store.edit { if (uri == null) it.remove(K.roms) else it[K.roms] = uri }
    suspend fun setTheme(v: ThemeMode) = ctx.store.edit { it[K.theme] = v.name }
    suspend fun setLayout(v: LibraryLayout) = ctx.store.edit { it[K.layout] = v.name }
    suspend fun setAccent(v: Int) = ctx.store.edit { it[K.accent] = v }
    suspend fun setCols(v: Int) = ctx.store.edit { it[K.cols] = v.coerceIn(2, 7) }
    suspend fun setRa(user: String, key: String) = ctx.store.edit { it[K.raUser] = user.trim(); it[K.raKey] = key.trim() }
    suspend fun setSgdb(key: String) = ctx.store.edit { it[K.sgdb] = key.trim() }
    suspend fun setDual(v: Boolean) = ctx.store.edit { it[K.dual] = v }
    suspend fun setHideNoArt(v: Boolean) = ctx.store.edit { it[K.hideNoArt] = v }
}
