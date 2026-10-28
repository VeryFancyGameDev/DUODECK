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
enum class LibraryLayout { GRID, CAROUSEL }
