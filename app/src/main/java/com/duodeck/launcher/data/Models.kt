package com.duodeck.launcher.data

import androidx.compose.ui.graphics.Color

data class Game(
    val id: String,          // document URI string, unique per ROM
    val title: String,       // cleaned display title
    val fileName: String,
    val systemId: String,
    val uri: String,
)

data class GameSystem(
    val id: String,
    val name: String,
    val short: String,
    val extensions: Set<String>,
    val folderAliases: Set<String>,
    val colorA: Color,
    val colorB: Color,
    /** Candidate emulator package names, first installed one wins. */
    val emulators: List<String>,
    /** Lower-case words matched against installed app labels (for apps whose package name we can't be sure of). */
    val labelHints: List<String> = emptyList(),
)

data class RaProgress(
    val title: String,
    val awarded: Int,
    val max: Int,
    val icon: String?,
)

data class RaProfile(
    val user: String,
    val points: Int,
    val softcore: Int,
    val rank: String,
    val avatarUrl: String?,
    val recent: List<String>,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class LibraryLayout { PAGES, GRID, CAROUSEL }
enum class MusicMode { OFF, BUILTIN, CUSTOM }
enum class LaunchMode { AUTO, VIEW_URI, DOLPHIN, RETROARCH }

data class AppEntry(val pkg: String, val label: String)

data class Group(
    val id: String,
    val name: String,
    val colorIdx: Int,
    val gameIds: List<String>,
)

/** Per-system (key = system id) or per-game (key = "game:<id>") emulator choice. */
data class LaunchProfile(
    val pkg: String? = null,
    val core: String? = null,
    val mode: LaunchMode = LaunchMode.AUTO,
)

data class Avatar(
    val name: String = "Player",
    val head: Int = 0,
    val skin: Int = 1,
    val hair: Int = 1,
    val hairColor: Int = 1,
    val eyes: Int = 0,
    val brows: Int = 1,
    val nose: Int = 0,
    val mouth: Int = 0,
    val glasses: Int = 0,
    val beard: Int = 0,
    val cheeks: Boolean = true,
    val shirt: Int = 0,
    val bg: Int = 0,
)

data class SteamGame(val appId: Int, val name: String)

data class GameStat(val plays: Int = 0, val last: Long = 0L)

enum class SortMode { NAME, RECENT, MOST_PLAYED }

/** Everything the user creates inside the app (saved to user.json). */
data class UserState(
    val coins: Int = 10,
    val coinDay: String = "",
    val coinsToday: Int = 0,
    val stats: Map<String, GameStat> = emptyMap(),
    val owned: Set<String> = emptySet(),
    val groups: List<Group> = emptyList(),
    val apps: List<AppEntry> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val customArt: Map<String, String> = emptyMap(),
    val profiles: Map<String, LaunchProfile> = emptyMap(),
    val avatar: Avatar = Avatar(),
    val themes: List<ThemeDef> = emptyList(),
    // --- step coins
    val stepBase: Long = -1L,
    val stepCarry: Int = 0,
    val stepsToday: Int = 0,
    val stepCoinsToday: Int = 0,
    val stepDay: String = "",
    val totalSteps: Long = 0L,
    // --- badge arcade
    val badges: Map<String, Int> = emptyMap(),
    val pins: List<String> = emptyList(),
    val clawPlays: Int = 0,
    // --- steam library (synced from the Steam Web API)
    val steam: List<SteamGame> = emptyList(),
)
