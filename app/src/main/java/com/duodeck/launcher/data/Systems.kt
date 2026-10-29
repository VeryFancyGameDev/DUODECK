package com.duodeck.launcher.data

import androidx.compose.ui.graphics.Color

private val RA = listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
private val DOLPHIN = listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")
private val SWITCH = listOf(
    "dev.eden.eden_emulator", "org.yuzu.yuzu_emu", "org.yuzu.yuzu_emu.ea",
    "org.citron.citron_emu", "org.sudachi.sudachi_emu", "org.stratoemu.strato", "skyline.emu",
)

object Systems {
    val all: List<GameSystem> = listOf(
        GameSystem("switch", "Nintendo Switch", "NSW", setOf("nsp", "xci", "nsz", "xcz"),
            setOf("switch", "nsw", "nintendoswitch"), Color(0xFFE60012), Color(0xFFFF6B6B), SWITCH),
        GameSystem("nes", "Nintendo Entertainment System", "NES", setOf("nes", "fds", "unf"),
            setOf("nes", "famicom", "fc"), Color(0xFFE53935), Color(0xFFFF8A65), RA),
        GameSystem("snes", "Super Nintendo", "SNES", setOf("sfc", "smc", "fig", "swc"),
            setOf("snes", "sfc", "superfamicom", "supernintendo"), Color(0xFF7E57C2), Color(0xFFB39DDB), RA),
        GameSystem("n64", "Nintendo 64", "N64", setOf("n64", "z64", "v64"),
            setOf("n64", "nintendo64"), Color(0xFF43A047), Color(0xFFA5D6A7), RA),
        GameSystem("gb", "Game Boy", "GB", setOf("gb"),
            setOf("gb", "gameboy"), Color(0xFF9CCC65), Color(0xFFDCE775), RA),
        GameSystem("gbc", "Game Boy Color", "GBC", setOf("gbc"),
            setOf("gbc", "gameboycolor"), Color(0xFFAB47BC), Color(0xFFF48FB1), RA),
        GameSystem("gba", "Game Boy Advance", "GBA", setOf("gba"),
            setOf("gba", "gameboyadvance"), Color(0xFF3949AB), Color(0xFF7986CB), RA),
        GameSystem("nds", "Nintendo DS", "NDS", setOf("nds", "dsi"),
            setOf("nds", "ds", "nintendods"), Color(0xFF90A4AE), Color(0xFFECEFF1),
            listOf("me.magnum.melonds", "com.dsemu.drastic") + RA),
        GameSystem("n3ds", "Nintendo 3DS", "3DS", setOf("3ds", "cia", "cci", "cxi"),
            setOf("3ds", "n3ds", "nintendo3ds"), Color(0xFF29B6F6), Color(0xFF81D4FA),
            listOf("org.azahar_emu.azahar", "io.github.lime3ds.android", "org.citra.citra_emu", "org.citra.emu")),
        GameSystem("gc", "GameCube", "GCN", setOf("gcm", "gcz", "rvz", "ciso", "iso", "wia"),
            setOf("gc", "gamecube", "ngc"), Color(0xFF5E35B1), Color(0xFF9575CD), DOLPHIN),
        GameSystem("wii", "Wii", "Wii", setOf("wbfs", "wad", "iso", "rvz", "wia", "ciso"),
            setOf("wii"), Color(0xFF26C6DA), Color(0xFF80DEEA), DOLPHIN),
        GameSystem("psx", "PlayStation", "PS1", setOf("cue", "bin", "chd", "pbp", "m3u", "img"),
            setOf("psx", "ps1", "playstation", "psone"), Color(0xFF546E7A), Color(0xFF90A4AE),
            listOf("com.github.stenzek.duckstation") + RA),
        GameSystem("ps2", "PlayStation 2", "PS2", setOf("iso", "chd", "cso", "gz"),
            setOf("ps2", "playstation2"), Color(0xFF1E88E5), Color(0xFF64B5F6),
            listOf("xyz.aethersx2.android", "com.izzy2lost.x2", "com.virtualapplications.play")),
        GameSystem("psp", "PlayStation Portable", "PSP", setOf("iso", "cso", "pbp"),
            setOf("psp", "playstationportable"), Color(0xFF3F51B5), Color(0xFF9FA8DA),
            listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")),
        GameSystem("genesis", "Sega Genesis", "GEN", setOf("md", "gen", "smd", "bin"),
            setOf("genesis", "megadrive", "md", "segagenesis"), Color(0xFF212121), Color(0xFF757575), RA),
        GameSystem("sms", "Sega Master System", "SMS", setOf("sms"),
            setOf("sms", "mastersystem"), Color(0xFF1565C0), Color(0xFF64B5F6), RA),
        GameSystem("gg", "Sega Game Gear", "GG", setOf("gg"),
            setOf("gg", "gamegear"), Color(0xFF455A64), Color(0xFF90A4AE), RA),
        GameSystem("dc", "Dreamcast", "DC", setOf("gdi", "cdi", "chd"),
            setOf("dc", "dreamcast"), Color(0xFFFB8C00), Color(0xFFFFCC80),
            listOf("io.recompiled.redream", "com.flycast.emulator", "com.reicast.emulator")),
        GameSystem("pce", "PC Engine", "PCE", setOf("pce"),
            setOf("pce", "pcengine", "turbografx16", "tg16"), Color(0xFFEF6C00), Color(0xFFFFB74D), RA),
        GameSystem("a2600", "Atari 2600", "2600", setOf("a26"),
            setOf("atari2600", "a2600"), Color(0xFF6D4C41), Color(0xFFBCAAA4), RA),
        GameSystem("arcade", "Arcade", "ARC", setOf("zip"),
            setOf("arcade", "mame", "fbneo", "neogeo"), Color(0xFFEC407A), Color(0xFFF8BBD0), RA),
    )

    private val byId = all.associateBy { it.id }
    private val byAlias: Map<String, GameSystem> =
        all.flatMap { s -> s.folderAliases.map { it to s } }.toMap()

    fun get(id: String): GameSystem? = byId[id]

    private fun key(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    /** Match a folder name like "Game Boy Advance" or "GBA" to a system. */
    fun fromFolder(name: String): GameSystem? {
        val k = key(name)
        return byAlias[k] ?: all.firstOrNull { key(it.name) == k }
    }

    /** Fallback when ROMs sit in an unnamed folder: pick by unique extension. */
    fun fromExtension(ext: String): GameSystem? = all.filter { ext in it.extensions }.singleOrNull()

    val allExtensions: Set<String> = all.flatMap { it.extensions }.toSet()
}
