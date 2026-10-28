package com.duodeck.launcher.data

import androidx.compose.ui.graphics.Color

private const val RETROARCH = "com.retroarch"
private const val RETROARCH_PLUS = "com.retroarch.aarch64"

object Systems {
    val all: List<GameSystem> = listOf(
        GameSystem("nes", "Nintendo Entertainment System", "NES", setOf("nes", "fds", "unf"),
            setOf("nes", "famicom", "fc"), Color(0xFFE53935), Color(0xFFFF8A65), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("snes", "Super Nintendo", "SNES", setOf("sfc", "smc", "fig", "swc"),
            setOf("snes", "sfc", "superfamicom", "supernintendo"), Color(0xFF7E57C2), Color(0xFFB39DDB), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("n64", "Nintendo 64", "N64", setOf("n64", "z64", "v64"),
            setOf("n64", "nintendo64"), Color(0xFF43A047), Color(0xFFA5D6A7), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("gb", "Game Boy", "GB", setOf("gb"),
            setOf("gb", "gameboy"), Color(0xFF9CCC65), Color(0xFFDCE775), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("gbc", "Game Boy Color", "GBC", setOf("gbc"),
            setOf("gbc", "gameboycolor"), Color(0xFFAB47BC), Color(0xFFF48FB1), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("gba", "Game Boy Advance", "GBA", setOf("gba"),
            setOf("gba", "gameboyadvance"), Color(0xFF3949AB), Color(0xFF7986CB), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("nds", "Nintendo DS", "NDS", setOf("nds", "dsi"),
            setOf("nds", "ds", "nintendods"), Color(0xFF90A4AE), Color(0xFFECEFF1), listOf("me.magnum.melonds", "com.dsemu.drastic", RETROARCH_PLUS)),
        GameSystem("n3ds", "Nintendo 3DS", "3DS", setOf("3ds", "cia", "cci", "cxi"),
            setOf("3ds", "n3ds", "nintendo3ds"), Color(0xFF29B6F6), Color(0xFF81D4FA), listOf("org.azahar_emu.azahar", "io.github.lime3ds.android", "org.citra.citra_emu", "org.citra.emu")),
        GameSystem("gc", "GameCube", "GCN", setOf("gcm", "gcz", "rvz", "ciso"),
            setOf("gc", "gamecube", "ngc"), Color(0xFF5E35B1), Color(0xFF9575CD), listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")),
        GameSystem("wii", "Wii", "Wii", setOf("wbfs", "wad", "iso"),
            setOf("wii"), Color(0xFF26C6DA), Color(0xFF80DEEA), listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")),
        GameSystem("psx", "PlayStation", "PS1", setOf("cue", "bin", "chd", "pbp", "m3u", "img"),
            setOf("psx", "ps1", "playstation", "psone"), Color(0xFF546E7A), Color(0xFF90A4AE), listOf("com.github.stenzek.duckstation", "com.retroarch.aarch64", RETROARCH)),
        GameSystem("ps2", "PlayStation 2", "PS2", setOf("iso", "chd", "cso", "gz"),
            setOf("ps2", "playstation2"), Color(0xFF1E88E5), Color(0xFF64B5F6), listOf("xyz.aethersx2.android", "com.izzy2lost.x2", "com.virtualapplications.play")),
        GameSystem("psp", "PlayStation Portable", "PSP", setOf("iso", "cso", "pbp"),
            setOf("psp", "playstationportable"), Color(0xFF3F51B5), Color(0xFF9FA8DA), listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")),
        GameSystem("genesis", "Sega Genesis", "GEN", setOf("md", "gen", "smd", "bin"),
            setOf("genesis", "megadrive", "md", "segagenesis"), Color(0xFF212121), Color(0xFF757575), listOf(RETROARCH_PLUS, RETROARCH)),
        GameSystem("dc", "Dreamcast", "DC", setOf("gdi", "cdi", "chd"),
            setOf("dc", "dreamcast"), Color(0xFFFB8C00), Color(0xFFFFCC80), listOf("io.recompiled.redream", "com.flycast.emulator", "com.reicast.emulator")),
        GameSystem("arcade", "Arcade", "ARC", setOf("zip"),
            setOf("arcade", "mame", "fbneo", "neogeo"), Color(0xFFEC407A), Color(0xFFF8BBD0), listOf(RETROARCH_PLUS, RETROARCH)),
    )

    private val byId = all.associateBy { it.id }
    private val byAlias: Map<String, GameSystem> =
        all.flatMap { s -> s.folderAliases.map { it to s } }.toMap()

    fun get(id: String): GameSystem? = byId[id]

    /** Match a folder name like "Game Boy Advance" or "GBA" to a system. */
    fun fromFolder(name: String): GameSystem? {
        val key = name.lowercase().filter { it.isLetterOrDigit() }
        return byAlias[key] ?: all.firstOrNull { it.name.lowercase().filter { c -> c.isLetterOrDigit() } == key }
    }

    /** Fallback when ROMs sit in an unnamed folder: pick by unique extension. */
    fun fromExtension(ext: String): GameSystem? {
        val matches = all.filter { ext in it.extensions }
        return matches.singleOrNull()
    }

    val allExtensions: Set<String> = all.flatMap { it.extensions }.toSet()
}
