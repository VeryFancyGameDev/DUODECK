package com.duodeck.launcher.data

/**
 * Emulators DuoDeck knows how to talk to. Activity names / extras are best-effort defaults;
 * every one of them can be overridden per system or per game in Settings → Emulators.
 */
data class KnownEmu(
    val pkg: String,
    val label: String,
    val activity: String? = null,
    val mode: LaunchMode = LaunchMode.VIEW_URI,
    val action: String? = null,
    /** If set, the game is also passed as this string extra. */
    val extra: String? = null,
    /** Pass a filesystem path in [extra] instead of the content URI. */
    val usePath: Boolean = false,
)

object Emulators {
    const val RA_ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"
    const val DOLPHIN_ACTIVITY = "org.dolphinemu.dolphinemu.ui.main.MainActivity"
    private const val YUZU = "org.yuzu.yuzu_emu.activities.EmulationActivity"

    val all: List<KnownEmu> = listOf(
        KnownEmu("com.retroarch.aarch64", "RetroArch (64-bit)", RA_ACTIVITY, LaunchMode.RETROARCH),
        KnownEmu("com.retroarch", "RetroArch", RA_ACTIVITY, LaunchMode.RETROARCH),
        KnownEmu("com.retroarch.ra32", "RetroArch (32-bit)", RA_ACTIVITY, LaunchMode.RETROARCH),

        KnownEmu("org.dolphinemu.dolphinemu", "Dolphin", DOLPHIN_ACTIVITY, LaunchMode.DOLPHIN),
        KnownEmu("org.dolphinemu.mmjr", "Dolphin MMJR", DOLPHIN_ACTIVITY, LaunchMode.DOLPHIN),

        KnownEmu("org.ppsspp.ppsspp", "PPSSPP", "org.ppsspp.ppsspp.PpssppActivity"),
        KnownEmu("org.ppsspp.ppssppgold", "PPSSPP Gold", "org.ppsspp.ppsspp.PpssppActivity"),

        KnownEmu("org.azahar_emu.azahar", "Azahar", "org.citra.citra_emu.activities.EmulationActivity"),
        KnownEmu("io.github.lime3ds.android", "Lime3DS", "org.citra.citra_emu.activities.EmulationActivity"),
        KnownEmu("org.citra.citra_emu", "Citra", "org.citra.citra_emu.activities.EmulationActivity"),
        KnownEmu("org.citra.emu", "Citra MMJ", "org.citra.emu.ui.EmulationActivity"),

        KnownEmu("me.magnum.melonds", "melonDS", "me.magnum.melonds.ui.emulator.EmulatorActivity",
            action = "me.magnum.melonds.LAUNCH_ROM", extra = "uri"),
        KnownEmu("com.dsemu.drastic", "DraStic", null),

        KnownEmu("com.github.stenzek.duckstation", "DuckStation",
            "com.github.stenzek.duckstation.EmulationActivity", extra = "bootPath"),
        KnownEmu("xyz.aethersx2.android", "AetherSX2 / NetherSX2", "xyz.aethersx2.android.EmulationActivity",
            action = "android.intent.action.MAIN", extra = "bootPath", usePath = true),
        KnownEmu("com.izzy2lost.x2", "ARMSX2", null),
        KnownEmu("com.virtualapplications.play", "Play!", null),

        KnownEmu("io.recompiled.redream", "Redream", "io.recompiled.redream.MainActivity"),
        KnownEmu("com.flycast.emulator", "Flycast", "com.flycast.emulator.MainActivity"),
        KnownEmu("com.reicast.emulator", "Reicast", null),

        // Nintendo Switch
        KnownEmu("dev.eden.eden_emulator", "Eden", YUZU),
        KnownEmu("org.yuzu.yuzu_emu", "Yuzu", YUZU),
        KnownEmu("org.yuzu.yuzu_emu.ea", "Yuzu Early Access", YUZU),
        KnownEmu("org.citron.citron_emu", "Citron", "org.citron.citron_emu.activities.EmulationActivity"),
        KnownEmu("org.sudachi.sudachi_emu", "Sudachi", "org.sudachi.sudachi_emu.activities.EmulationActivity"),
        KnownEmu("org.stratoemu.strato", "Strato", "emu.skyline.EmulationActivity"),
        KnownEmu("skyline.emu", "Skyline", "emu.skyline.EmulationActivity"),
    )

    val byPkg: Map<String, KnownEmu> = all.associateBy { it.pkg }

    val retroarchPkgs: Set<String> = all.filter { it.mode == LaunchMode.RETROARCH }.map { it.pkg }.toSet()

    /** RetroArch cores worth offering per system. First entry is the default. */
    private val cores: Map<String, List<String>> = mapOf(
        "nes" to listOf("fceumm", "nestopia", "mesen"),
        "snes" to listOf("snes9x", "snes9x2010", "bsnes"),
        "n64" to listOf("mupen64plus_next", "parallel_n64"),
        "gb" to listOf("gambatte", "sameboy", "mgba"),
        "gbc" to listOf("gambatte", "sameboy", "mgba"),
        "gba" to listOf("mgba", "gpsp", "vba_next"),
        "nds" to listOf("melonds", "desmume"),
        "psx" to listOf("pcsx_rearmed", "swanstation", "mednafen_psx_hw"),
        "psp" to listOf("ppsspp"),
        "genesis" to listOf("genesis_plus_gx", "picodrive"),
        "sms" to listOf("genesis_plus_gx", "picodrive", "smsplus"),
        "gg" to listOf("genesis_plus_gx", "smsplus"),
        "pce" to listOf("mednafen_pce_fast", "mednafen_pce"),
        "a2600" to listOf("stella"),
        "dc" to listOf("flycast"),
        "arcade" to listOf("fbneo", "mame2003_plus", "mame"),
    )

    fun coresFor(systemId: String): List<String> = cores[systemId].orEmpty()
}
