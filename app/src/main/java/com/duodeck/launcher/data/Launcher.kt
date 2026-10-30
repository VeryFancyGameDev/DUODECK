package com.duodeck.launcher.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast

data class Resolved(val pkg: String?, val known: KnownEmu?, val mode: LaunchMode, val core: String?)

/** Starts a game in whichever emulator fits (user override → first installed known emulator). */
object Launcher {

    private const val GRANT = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    private var labelCache: List<Pair<String, String>> = emptyList()
    private var labelCacheAt = 0L

    /** (package, label) of every launchable app, cached for a minute. */
    private fun appLabels(ctx: Context): List<Pair<String, String>> {
        val now = System.currentTimeMillis()
        if (now - labelCacheAt > 60_000 || labelCache.isEmpty()) {
            val pm = ctx.packageManager
            labelCache = runCatching {
                pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                    .distinctBy { it.first }
            }.getOrDefault(emptyList())
            labelCacheAt = now
        }
        return labelCache
    }

    /** Installed apps whose name contains one of the system's hint words (e.g. "watermelon", "gamehub"). */
    fun hinted(ctx: Context, system: GameSystem): List<KnownEmu> {
        if (system.labelHints.isEmpty()) return emptyList()
        return appLabels(ctx).filter { (pkg, label) ->
            val l = label.lowercase()
            val words = l.split(' ', '-', '_', '.')
            pkg != ctx.packageName && system.labelHints.any { h -> if (' ' in h) l.contains(h) else words.any { it.startsWith(h) } }
        }.map { (pkg, label) -> Emulators.byPkg[pkg] ?: KnownEmu(pkg, label) }
    }

    /** Known emulators for [system] that are actually installed, plus apps found by name. */
    fun candidates(ctx: Context, system: GameSystem): List<KnownEmu> =
        (system.emulators.mapNotNull { Emulators.byPkg[it] }.filter { isInstalled(ctx, it.pkg) } + hinted(ctx, system))
            .distinctBy { it.pkg }

    fun installed(ctx: Context, system: GameSystem): String? = candidates(ctx, system).firstOrNull()?.pkg

    fun resolve(ctx: Context, systemId: String, gameId: String?, user: UserState): Resolved {
        val system = Systems.get(systemId)
        val profile = (gameId?.let { user.profiles["game:$it"] }) ?: user.profiles[systemId] ?: LaunchProfile()
        val pkg = profile.pkg?.takeIf { isInstalled(ctx, it) } ?: system?.let { installed(ctx, it) }
        val known = pkg?.let { p -> Emulators.byPkg[p] ?: system?.let { s -> hinted(ctx, s).firstOrNull { it.pkg == p } } }
        val mode = if (profile.mode != LaunchMode.AUTO) profile.mode else known?.mode ?: LaunchMode.VIEW_URI
        val core = profile.core ?: Emulators.coresFor(systemId).firstOrNull()
        return Resolved(pkg, known, mode, core)
    }

    fun launchGame(ctx: Context, game: Game, user: UserState, forceChooser: Boolean = false) {
        if (game.systemId == "pc") { launchPc(ctx, game, user); return }
        val r = resolve(ctx, game.systemId, game.id, user)
        val uri = Uri.parse(game.uri)

        if (r.pkg == null || forceChooser) {
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(GRANT)
            view.clipData = ClipData.newRawUri("rom", uri)
            runCatching {
                ctx.startActivity(Intent.createChooser(view, "Open ${game.title} with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { toast(ctx, "No app found for ${Systems.get(game.systemId)?.short ?: "this game"}") }
            return
        }

        for (intent in attempts(r, r.pkg, game, uri)) {
            try {
                ctx.startActivity(intent)
                return
            } catch (_: Exception) { /* try the next form */ }
        }
        // Last resort: just open the emulator so the user isn't left staring at DuoDeck.
        ctx.packageManager.getLaunchIntentForPackage(r.pkg)?.let {
            ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            toast(ctx, "Opened ${r.known?.label ?: r.pkg}, but it wouldn't take the game. Try another launch style in Settings → Emulators.")
        } ?: toast(ctx, "Couldn't start the emulator")
    }

    /**
     * Steam games. GameNative has a documented launch action (app.gamenative.LAUNCH_GAME + app_id), so those
     * start straight into the game. Other launchers (GameHub, Winlator, Droid Deck…) have no public intent
     * we can rely on, so we open the app and tell you which game to start.
     */
    private fun launchPc(ctx: Context, game: Game, user: UserState) {
        val appId = game.uri.removePrefix("steam://").toIntOrNull()
        val r = resolve(ctx, "pc", game.id, user)
        val pkg = r.pkg
        if (pkg == null) { toast(ctx, "Install GameNative, GameHub or Winlator to play PC games"); return }
        if (pkg == Emulators.GAMENATIVE && appId != null && appId > 0) {
            val i = Intent("app.gamenative.LAUNCH_GAME").setPackage(pkg)
                .putExtra("app_id", appId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i) }.isSuccess) return
        }
        launchApp(ctx, pkg)
        toast(ctx, "Opened ${r.known?.label ?: pkg}. Start “${game.title}” from there.")
    }

    fun launchApp(ctx: Context, pkg: String) {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) { toast(ctx, "That app isn't installed"); return }
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // ---------------------------------------------------------------- intents

    private fun attempts(r: Resolved, pkg: String, game: Game, uri: Uri): List<Intent> {
        val known = r.known
        return when (r.mode) {
            LaunchMode.RETROARCH -> listOfNotNull(retroArch(pkg, known, r.core, game.systemId, uri))
                .plus(view(pkg, known, uri, withComponent = false, withType = true))
            LaunchMode.DOLPHIN -> listOf(
                dolphin(pkg, known, uri, withComponent = true),
                dolphin(pkg, known, uri, withComponent = false),
                view(pkg, known, uri, withComponent = false, withType = false),
            )
            else -> listOf(
                view(pkg, known, uri, withComponent = true, withType = true),
                view(pkg, known, uri, withComponent = false, withType = true),
                view(pkg, known, uri, withComponent = false, withType = false),
            )
        }
    }

    private fun view(pkg: String, known: KnownEmu?, uri: Uri, withComponent: Boolean, withType: Boolean): Intent =
        Intent(known?.action ?: Intent.ACTION_VIEW).apply {
            val act = known?.activity
            if (withComponent && act != null) setClassName(pkg, act) else setPackage(pkg)
            if (withType) setDataAndType(uri, "*/*") else data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or GRANT)
            clipData = ClipData.newRawUri("rom", uri)
            known?.extra?.let { key ->
                putExtra(key, if (known?.usePath == true) (toFilePath(uri) ?: uri.toString()) else uri.toString())
            }
        }

    /**
     * Dolphin reads the game from ACTION_VIEW data and/or the AutoStartFiles extra on its main
     * activity. We send both, with a URI grant on the clip data so the content:// URI is readable.
     */
    private fun dolphin(pkg: String, known: KnownEmu?, uri: Uri, withComponent: Boolean): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            if (withComponent) setClassName(pkg, known?.activity ?: Emulators.DOLPHIN_ACTIVITY) else setPackage(pkg)
            data = uri
            putExtra("AutoStartFiles", arrayOf(uri.toString()))
            putExtra("AutoStartFile", uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or GRANT)
            clipData = ClipData.newRawUri("rom", uri)
        }

    /** RetroArch needs a real file path plus the core to load. */
    private fun retroArch(pkg: String, known: KnownEmu?, core: String?, systemId: String, uri: Uri): Intent? {
        val path = toFilePath(uri) ?: return null
        val c = core ?: Emulators.coresFor(systemId).firstOrNull() ?: return null
        return Intent().apply {
            setClassName(pkg, known?.activity ?: Emulators.RA_ACTIVITY)
            putExtra("ROM", path)
            putExtra("LIBRETRO", "/data/data/$pkg/cores/${c}_libretro_android.so")
            putExtra("CONFIGFILE", "/storage/emulated/0/Android/data/$pkg/files/retroarch.cfg")
            putExtra("QUITFOCUS", "")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /** content://…/document/primary:Roms/GBA/x.gba  ->  /storage/emulated/0/Roms/GBA/x.gba */
    fun toFilePath(uri: Uri): String? {
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
        val parts = docId.split(':', limit = 2)
        if (parts.size < 2) return null
        return if (parts[0] == "primary") "/storage/emulated/0/${parts[1]}" else "/storage/${parts[0]}/${parts[1]}"
    }

    private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
}
