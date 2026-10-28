package com.duodeck.launcher.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Scans a Storage Access Framework tree (the folder the user picked) for ROMs.
 * Uses raw DocumentsContract queries, which are far faster than DocumentFile.
 */
object RomScanner {

    suspend fun scan(ctx: Context, treeUri: Uri, onProgress: (Int) -> Unit = {}): List<Game> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<Game>()
            val rootId = DocumentsContract.getTreeDocumentId(treeUri)
            walk(ctx, treeUri, rootId, Systems.fromFolder(rootId.substringAfterLast('/').substringAfterLast(':')), 0, out, onProgress)
            out.sortedBy { it.title.lowercase() }
        }

    private fun walk(
        ctx: Context, tree: Uri, docId: String, inherited: GameSystem?,
        depth: Int, out: MutableList<Game>, onProgress: (Int) -> Unit,
    ) {
        if (depth > 6) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val dirs = ArrayList<Pair<String, String>>()
        ctx.contentResolver.query(children, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    dirs += id to name
                    continue
                }
                if (name.startsWith(".")) continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in Systems.allExtensions) continue
                val system = inherited ?: Systems.fromExtension(ext) ?: continue
                if (ext !in system.extensions) continue
                // .bin/.img next to a .cue are duplicates; keep only the .cue
                if (ext == "bin" && system.id == "psx") continue
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                out += Game(uri.toString(), cleanTitle(name), name, system.id, uri.toString())
                if (out.size % 25 == 0) onProgress(out.size)
            }
        }
        for ((id, name) in dirs) {
            walk(ctx, tree, id, Systems.fromFolder(name) ?: inherited, depth + 1, out, onProgress)
        }
    }

    fun cleanTitle(file: String): String =
        file.substringBeforeLast('.')
            .replace(Regex("\\s*[\\(\\[][^\\)\\]]*[\\)\\]]"), "")   // (USA) [!] etc.
            .replace('_', ' ')
            .trim()
            .ifEmpty { file }

    // ---- tiny JSON cache so the library appears instantly on launch ----
    private fun cacheFile(ctx: Context) = File(ctx.filesDir, "library.json")

    fun saveCache(ctx: Context, games: List<Game>) {
        val arr = JSONArray()
        games.forEach {
            arr.put(JSONObject().put("i", it.id).put("t", it.title).put("f", it.fileName).put("s", it.systemId))
        }
        cacheFile(ctx).writeText(arr.toString())
    }

    fun loadCache(ctx: Context): List<Game> = runCatching {
        val arr = JSONArray(cacheFile(ctx).readText())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Game(o.getString("i"), o.getString("t"), o.getString("f"), o.getString("s"), o.getString("i"))
        }
    }.getOrDefault(emptyList())
}
