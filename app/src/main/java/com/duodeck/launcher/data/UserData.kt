package com.duodeck.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves/loads [UserState] as a small JSON file in the app's private storage. */
object UserStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "user.json")

    fun load(ctx: Context): UserState = runCatching {
        parse(JSONObject(file(ctx).readText()))
    }.getOrDefault(UserState())

    fun save(ctx: Context, s: UserState) {
        runCatching { file(ctx).writeText(toJson(s).toString()) }
    }

    private fun strings(a: JSONArray?): List<String> =
        (0 until (a?.length() ?: 0)).map { a!!.getString(it) }

    private fun parse(o: JSONObject): UserState {
        val groups = o.optJSONArray("groups")?.let { a ->
            (0 until a.length()).map {
                val g = a.getJSONObject(it)
                Group(g.getString("id"), g.getString("name"), g.optInt("color"), strings(g.optJSONArray("games")))
            }
        }.orEmpty()
        val apps = o.optJSONArray("apps")?.let { a ->
            (0 until a.length()).map { AppEntry(a.getJSONObject(it).getString("pkg"), a.getJSONObject(it).getString("label")) }
        }.orEmpty()
        val art = o.optJSONObject("art")?.let { j -> j.keys().asSequence().associateWith { j.getString(it) } }.orEmpty()
        val profiles = o.optJSONObject("profiles")?.let { j ->
            j.keys().asSequence().associateWith {
                val p = j.getJSONObject(it)
                LaunchProfile(
                    pkg = p.optString("pkg").ifEmpty { null },
                    core = p.optString("core").ifEmpty { null },
                    mode = runCatching { LaunchMode.valueOf(p.optString("mode", "AUTO")) }.getOrDefault(LaunchMode.AUTO),
                )
            }
        }.orEmpty()
        val themes = o.optJSONArray("themes")?.let { a ->
            (0 until a.length()).mapNotNull { themeFromJson(a.getJSONObject(it)) }
        }.orEmpty()
        return UserState(
            groups = groups, apps = apps,
            favorites = strings(o.optJSONArray("favorites")).toSet(),
            recents = strings(o.optJSONArray("recents")),
            customArt = art, profiles = profiles,
            avatar = o.optJSONObject("avatar")?.let(::avatarFrom) ?: Avatar(),
            themes = themes,
        )
    }

    private fun toJson(s: UserState): JSONObject = JSONObject().apply {
        put("groups", JSONArray().apply {
            s.groups.forEach { g ->
                put(JSONObject().put("id", g.id).put("name", g.name).put("color", g.colorIdx)
                    .put("games", JSONArray(g.gameIds)))
            }
        })
        put("apps", JSONArray().apply { s.apps.forEach { put(JSONObject().put("pkg", it.pkg).put("label", it.label)) } })
        put("favorites", JSONArray(s.favorites.toList()))
        put("recents", JSONArray(s.recents))
        put("art", JSONObject().apply { s.customArt.forEach { (k, v) -> put(k, v) } })
        put("profiles", JSONObject().apply {
            s.profiles.forEach { (k, p) ->
                put(k, JSONObject().put("pkg", p.pkg ?: "").put("core", p.core ?: "").put("mode", p.mode.name))
            }
        })
        put("avatar", avatarJson(s.avatar))
        put("themes", JSONArray().apply { s.themes.forEach { put(it.toJson()) } })
    }

    private fun avatarJson(a: Avatar) = JSONObject()
        .put("name", a.name).put("head", a.head).put("skin", a.skin).put("hair", a.hair)
        .put("hairColor", a.hairColor).put("eyes", a.eyes).put("brows", a.brows).put("nose", a.nose)
        .put("mouth", a.mouth).put("glasses", a.glasses).put("beard", a.beard).put("cheeks", a.cheeks)
        .put("shirt", a.shirt).put("bg", a.bg)

    private fun avatarFrom(o: JSONObject): Avatar {
        val d = Avatar()
        return Avatar(
            name = o.optString("name", d.name), head = o.optInt("head", d.head), skin = o.optInt("skin", d.skin),
            hair = o.optInt("hair", d.hair), hairColor = o.optInt("hairColor", d.hairColor),
            eyes = o.optInt("eyes", d.eyes), brows = o.optInt("brows", d.brows), nose = o.optInt("nose", d.nose),
            mouth = o.optInt("mouth", d.mouth), glasses = o.optInt("glasses", d.glasses),
            beard = o.optInt("beard", d.beard), cheeks = o.optBoolean("cheeks", d.cheeks),
            shirt = o.optInt("shirt", d.shirt), bg = o.optInt("bg", d.bg),
        )
    }
}
