package com.duodeck.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One colour set (a theme has a light and a dark one). Colours are ARGB ints. */
data class Pal(
    val bgTop: Int, val bgBottom: Int, val dots: Int,
    val surface: Int, val surfaceHigh: Int,
    val text: Int, val muted: Int,
    val accent: Int, val accent2: Int, val stroke: Int,
)

data class ThemeDef(
    val id: String,
    val name: String,
    val author: String,
    val blurb: String,
    /** dots | lines | grid | bubbles | none */
    val pattern: String,
    val light: Pal,
    val dark: Pal,
    val builtin: Boolean = false,
)

private fun hex(i: Int) = String.format("#%08X", i)
private fun parse(s: String?, fallback: Int): Int =
    runCatching { android.graphics.Color.parseColor(s) }.getOrDefault(fallback)

private fun Pal.toJson() = JSONObject()
    .put("bgTop", hex(bgTop)).put("bgBottom", hex(bgBottom)).put("dots", hex(dots))
    .put("surface", hex(surface)).put("surfaceHigh", hex(surfaceHigh))
    .put("text", hex(text)).put("muted", hex(muted))
    .put("accent", hex(accent)).put("accent2", hex(accent2)).put("stroke", hex(stroke))

private fun palFrom(o: JSONObject?, f: Pal): Pal = if (o == null) f else Pal(
    parse(o.optString("bgTop"), f.bgTop), parse(o.optString("bgBottom"), f.bgBottom), parse(o.optString("dots"), f.dots),
    parse(o.optString("surface"), f.surface), parse(o.optString("surfaceHigh"), f.surfaceHigh),
    parse(o.optString("text"), f.text), parse(o.optString("muted"), f.muted),
    parse(o.optString("accent"), f.accent), parse(o.optString("accent2"), f.accent2), parse(o.optString("stroke"), f.stroke),
)

fun ThemeDef.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("author", author).put("blurb", blurb).put("pattern", pattern)
    .put("light", light.toJson()).put("dark", dark.toJson())

fun themeFromJson(o: JSONObject): ThemeDef? {
    val id = o.optString("id").trim()
    val name = o.optString("name").trim()
    if (id.isEmpty() || name.isEmpty()) return null
    val base = Themes.builtin.first()
    return ThemeDef(
        id = id, name = name,
        author = o.optString("author", "Unknown"),
        blurb = o.optString("blurb", ""),
        pattern = o.optString("pattern", "dots"),
        light = palFrom(o.optJSONObject("light"), base.light),
        dark = palFrom(o.optJSONObject("dark"), base.dark),
    )
}

private fun P(
    bgTop: Long, bgBottom: Long, dots: Long, surface: Long, high: Long,
    text: Long, muted: Long, accent: Long, accent2: Long, stroke: Long,
) = Pal(bgTop.toInt(), bgBottom.toInt(), dots.toInt(), surface.toInt(), high.toInt(),
    text.toInt(), muted.toInt(), accent.toInt(), accent2.toInt(), stroke.toInt())

object Themes {
    /** Where the in-app Theme Store looks for new themes. Anyone can add a theme by adding to this file. */
    const val STORE_URL = "https://raw.githubusercontent.com/VeryFancyGameDev/duodeck/main/themes/index.json"

    val builtin: List<ThemeDef> = listOf(
        ThemeDef("sky", "Sky", "DuoDeck", "Bright 3DS-style blue with halftone dots.", "dots",
            light = P(0xFFF7FBFF, 0xFFDCEBF7, 0x1A3D8EC9, 0xFFFFFFFF, 0xFFEAF3FB, 0xFF26324A, 0xFF6F809E, 0xFF1FA8E0, 0xFF6FD6FF, 0x1F26324A),
            dark = P(0xFF1B2540, 0xFF0E1426, 0x14FFFFFF, 0xFF243055, 0xFF2E3C6B, 0xFFF1F5FF, 0xFF9FB0D8, 0xFF1FA8E0, 0xFF6FD6FF, 0x33FFFFFF),
            builtin = true),
        ThemeDef("shiro", "Shiro & Kuro", "DuoDeck", "Clean Wii U-style white and charcoal with fine lines.", "lines",
            light = P(0xFFFAFBFC, 0xFFE3E8EE, 0x14587A99, 0xFFFFFFFF, 0xFFEEF2F6, 0xFF2B3440, 0xFF7A8794, 0xFF21B5E8, 0xFF7ED8F8, 0x1F2B3440),
            dark = P(0xFF1A1F27, 0xFF0C0F14, 0x12FFFFFF, 0xFF262D38, 0xFF313A48, 0xFFF3F6FA, 0xFF9AA6B5, 0xFF29C5F6, 0xFF82E0FF, 0x33FFFFFF),
            builtin = true),
        ThemeDef("sakura", "Sakura", "DuoDeck", "Soft blossom pink.", "bubbles",
            light = P(0xFFFFF5F9, 0xFFFADDE9, 0x22E8509A, 0xFFFFFFFF, 0xFFFDEBF3, 0xFF4A2A3A, 0xFF9B6F83, 0xFFE8509A, 0xFFFF9CCB, 0x1F4A2A3A),
            dark = P(0xFF2E1A2A, 0xFF170B16, 0x14FFB3D4, 0xFF3C2336, 0xFF4B2D44, 0xFFFFEEF5, 0xFFD2A3BB, 0xFFFF6FB0, 0xFFFFB3D6, 0x33FFFFFF),
            builtin = true),
        ThemeDef("mint", "Mint Fresh", "DuoDeck", "Cool green and teal.", "dots",
            light = P(0xFFF3FFFA, 0xFFD3F2E6, 0x1A1DB98A, 0xFFFFFFFF, 0xFFE6F8F0, 0xFF1F3A33, 0xFF5F8A7D, 0xFF1DB98A, 0xFF7DEBC2, 0x1F1F3A33),
            dark = P(0xFF12302B, 0xFF08181A, 0x14A5F5DA, 0xFF1B433C, 0xFF235449, 0xFFEAFFF7, 0xFF8FCDB9, 0xFF2ED3A0, 0xFF8FF2CF, 0x33FFFFFF),
            builtin = true),
        ThemeDef("sunny", "Sunny Day", "DuoDeck", "Warm orange and yellow.", "bubbles",
            light = P(0xFFFFFAF0, 0xFFFFE6B8, 0x22F29B1D, 0xFFFFFFFF, 0xFFFFF1D6, 0xFF45310F, 0xFF957A47, 0xFFF29B1D, 0xFFFFD36B, 0x1F45310F),
            dark = P(0xFF33220C, 0xFF1A1105, 0x14FFD36B, 0xFF42301A, 0xFF553F22, 0xFFFFF4DD, 0xFFD8BC85, 0xFFFFA928, 0xFFFFD98A, 0x33FFFFFF),
            builtin = true),
        ThemeDef("grape", "Grape Soda", "DuoDeck", "Playful purple.", "dots",
            light = P(0xFFF8F5FF, 0xFFE3DAFA, 0x227A55E0, 0xFFFFFFFF, 0xFFEFE9FD, 0xFF2F2650, 0xFF7E73A6, 0xFF7A55E0, 0xFFB79CFF, 0x1F2F2650),
            dark = P(0xFF241B45, 0xFF110C26, 0x14C9B5FF, 0xFF2F2559, 0xFF3C3070, 0xFFF3EEFF, 0xFFB5A9E0, 0xFF8F6BFF, 0xFFC4AEFF, 0x33FFFFFF),
            builtin = true),
        ThemeDef("arcade", "Neon Arcade", "DuoDeck", "Dark synthwave grid with neon accents.", "grid",
            light = P(0xFFF5F0FF, 0xFFE0D4FF, 0x1F6A35FF, 0xFFFFFFFF, 0xFFEDE5FF, 0xFF241548, 0xFF7C6BA8, 0xFFE0268F, 0xFF28D7E8, 0x1F241548),
            dark = P(0xFF1A0B3B, 0xFF0A0420, 0x2228D7E8, 0xFF261052, 0xFF331768, 0xFFF6EEFF, 0xFFB79AF0, 0xFFFF2E9A, 0xFF35E8F5, 0x4435E8F5),
            builtin = true),
    )

    /** Fetches the online store. Returns an empty list when offline. */
    suspend fun fetchStore(): Result<List<ThemeDef>> = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(STORE_URL).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val text = try {
                if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().readText()
            } finally { c.disconnect() }
            val arr = JSONObject(text).optJSONArray("themes") ?: JSONArray()
            (0 until arr.length()).mapNotNull { themeFromJson(arr.getJSONObject(it)) }
        }
    }

    /** Parses a pasted/imported theme file (one theme object, or {"themes":[…]}). */
    fun parseImport(text: String): List<ThemeDef> = runCatching {
        val t = text.trim()
        if (t.startsWith("[")) {
            val a = JSONArray(t); (0 until a.length()).mapNotNull { themeFromJson(a.getJSONObject(it)) }
        } else {
            val o = JSONObject(t)
            val arr = o.optJSONArray("themes")
            if (arr != null) (0 until arr.length()).mapNotNull { themeFromJson(arr.getJSONObject(it)) }
            else listOfNotNull(themeFromJson(o))
        }
    }.getOrDefault(emptyList())
}
