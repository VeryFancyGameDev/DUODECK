package com.duodeck.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast

/** Starts a game in whichever installed emulator fits the system. Best-effort; see README. */
object Launcher {

    private val retroarchCores = mapOf(
        "nes" to "fceumm", "snes" to "snes9x", "gb" to "gambatte", "gbc" to "gambatte",
        "gba" to "mgba", "genesis" to "genesis_plus_gx", "n64" to "mupen64plus_next",
        "psx" to "pcsx_rearmed", "arcade" to "fbneo", "nds" to "melonds", "psp" to "ppsspp",
    )

    fun installed(ctx: Context, system: GameSystem): String? =
        system.emulators.firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }

    fun launch(ctx: Context, game: Game, forceChooser: Boolean = false) {
        val system = Systems.get(game.systemId) ?: return
        val pkg = installed(ctx, system)
        val uri = Uri.parse(game.uri)

        if (pkg == null || forceChooser) {
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { ctx.startActivity(Intent.createChooser(view, "Open ${game.title} with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { toast(ctx, "No emulator found for ${system.short}") }
            return
        }

        val intent = (if (pkg.startsWith("com.retroarch")) retroArchIntent(pkg, system, uri) else null)
            ?: Intent(Intent.ACTION_VIEW).setPackage(pkg).setDataAndType(uri, "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { ctx.startActivity(intent) }.onFailure {
            // Fall back to simply opening the emulator.
            ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
                ?: toast(ctx, "Couldn't start the emulator")
        }
    }

    /** RetroArch wants real file paths + a core, so convert the SAF URI where possible. */
    private fun retroArchIntent(pkg: String, system: GameSystem, uri: Uri): Intent? {
        val path = toFilePath(uri) ?: return null
        val core = retroarchCores[system.id] ?: return null
        val dataDir = "/data/data/$pkg"
        return Intent().apply {
            component = ComponentName(pkg, "com.retroarch.browser.retroactivity.RetroActivityFuture")
            putExtra("ROM", path)
            putExtra("LIBRETRO", "$dataDir/cores/${core}_libretro_android.so")
            putExtra("CONFIGFILE", "/storage/emulated/0/Android/data/$pkg/files/retroarch.cfg")
            putExtra("QUITFOCUS", "")
        }
    }

    /** content://…/document/primary:Roms/GBA/x.gba  ->  /storage/emulated/0/Roms/GBA/x.gba */
    fun toFilePath(uri: Uri): String? {
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
        val (volume, rel) = docId.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return if (volume == "primary") "/storage/emulated/0/$rel" else "/storage/$volume/$rel"
    }

    private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
