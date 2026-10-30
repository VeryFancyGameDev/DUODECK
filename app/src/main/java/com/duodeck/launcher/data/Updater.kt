package com.duodeck.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val build: Long, val name: String, val notes: String, val apkUrl: String, val size: Long)

/**
 * Updates straight from this project's GitHub Releases, so you never have to dig through Actions again.
 * Every successful build publishes a release tagged build-<number>; the app compares that number with its own.
 */
object Updater {
    private const val REPO = "VeryFancyGameDev/duodeck"

    fun installedBuild(ctx: Context): Long = runCatching {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
    }.getOrDefault(0L)

    fun installedName(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
    }.getOrNull() ?: "?"

    /** Returns the newest release (or null if the check failed). [UpdateInfo.build] > installedBuild means newer. */
    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            if (c.responseCode != 200) return@runCatching null
            val o = JSONObject(c.inputStream.bufferedReader().readText())
            val tag = o.optString("tag_name")
            val build = tag.substringAfterLast('-').toLongOrNull() ?: return@runCatching null
            val assets = o.optJSONArray("assets") ?: return@runCatching null
            var url: String? = null; var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { url = a.optString("browser_download_url"); size = a.optLong("size"); break }
            }
            UpdateInfo(build, o.optString("name").ifBlank { tag }, o.optString("body"), url ?: return@runCatching null, size)
        }.getOrNull()
    }

    /** Downloads the APK, reporting 0..1 progress. Returns the file or null. */
    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
            val out = File(dir, "DuoDeck-${info.build}.apk")
            val c = URL(info.apkUrl).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 30000; c.instanceFollowRedirects = true
            val total = c.contentLengthLong.takeIf { it > 0 } ?: info.size
            c.inputStream.use { i ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var done = 0L
                    while (true) {
                        val n = i.read(buf); if (n < 0) break
                        o.write(buf, 0, n); done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            out.takeIf { it.length() > 100_000 }
        }.getOrNull()
    }

    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
