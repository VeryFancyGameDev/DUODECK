package com.duodeck.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duodeck.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { HOME, SETTINGS, THEMES, AVATAR, EMULATORS }

sealed interface Tile { val key: String }
data class GameTile(val game: Game) : Tile { override val key: String get() = "g:${game.id}" }
data class GroupTile(val group: Group) : Tile { override val key: String get() = "grp:${group.id}" }
data class AppTile(val app: AppEntry) : Tile { override val key: String get() = "app:${app.pkg}" }
data class AddTile(val kind: String) : Tile { override val key: String get() = "add:$kind" }

sealed interface Dlg {
    data class GameMenu(val game: Game) : Dlg
    data class GroupMenu(val group: Group) : Dlg
    data class AppMenu(val app: AppEntry) : Dlg
    data class ArtPicker(val game: Game) : Dlg
    data class AddToGroup(val game: Game) : Dlg
    data class NewGroup(val addGameId: String? = null) : Dlg
    data class RenameGroup(val group: Group) : Dlg
    data object AddApp : Dlg
    data class EmuProfile(val key: String, val systemId: String, val title: String) : Dlg
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application get() = getApplication()
    val prefs = Prefs(app)
    val sgdb = SteamGridDb(app)

    val settings = prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    // ------------------------------------------------------------ user-made data
    private val _user = MutableStateFlow(UserStore.load(app))
    val user: StateFlow<UserState> = _user

    fun updateUser(f: (UserState) -> UserState) {
        _user.update(f)
        val snapshot = _user.value
        viewModelScope.launch(Dispatchers.IO) { UserStore.save(ctx, snapshot) }
    }

    // ------------------------------------------------------------ library
    private val _games = MutableStateFlow(RomScanner.loadCache(app))
    val games: StateFlow<List<Game>> = _games

    private val autoArt = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Custom picks win over automatic SteamGridDB covers. */
    val art: StateFlow<Map<String, String>> =
        combine(autoArt, _user) { a, u -> a + u.customArt }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning
    val scanCount = MutableStateFlow(0)

    val systemsPresent: StateFlow<List<GameSystem>> = games.map { g ->
        val ids = g.map { it.systemId }.toSet()
        Systems.all.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ------------------------------------------------------------ navigation / dialogs
    val screen = MutableStateFlow(Screen.HOME)
    private val backStack = ArrayDeque<Screen>()
    val dialog = MutableStateFlow<Dlg?>(null)

    fun go(s: Screen) { backStack.addLast(screen.value); screen.value = s }
    fun back(): Boolean {
        if (backStack.isEmpty()) { screen.value = Screen.HOME; return false }
        screen.value = backStack.removeLast(); return true
    }
    fun home() { backStack.clear(); screen.value = Screen.HOME }
    fun show(d: Dlg?) { dialog.value = d }

    // ------------------------------------------------------------ tabs, search, tiles
    /** all | fav | recent | groups | apps | sys:<id> | group:<id> */
    val tab = MutableStateFlow("all")
    val query = MutableStateFlow("")
    val selectedKey = MutableStateFlow<String?>(null)

    private data class Inputs(
        val games: List<Game>, val tab: String, val q: String, val user: UserState, val art: Map<String, String>,
    )

    val tiles: StateFlow<List<Tile>> =
        combine(combine(games, tab, query, _user, art) { g, t, q, u, a -> Inputs(g, t, q, u, a) }, settings) { i, s ->
            buildTiles(i, s)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun buildTiles(i: Inputs, s: Settings): List<Tile> {
        val q = i.q.trim()
        fun ok(t: String) = q.isEmpty() || t.contains(q, ignoreCase = true)
        val byId = i.games.associateBy { it.id }
        fun sorted(list: List<Game>): List<Game> = when (s.sort) {
            SortMode.NAME -> list
            SortMode.RECENT -> list.sortedByDescending { i.user.stats[it.id]?.last ?: 0L }
            SortMode.MOST_PLAYED -> list.sortedByDescending { i.user.stats[it.id]?.plays ?: 0 }
        }
        fun gameTiles(list: List<Game>): List<Tile> =
            list.filter { ok(it.title) && (!s.hideNoArt || i.art.containsKey(it.id)) }.map { GameTile(it) }
        val t = i.tab
        return when {
            t == "groups" -> i.user.groups.filter { ok(it.name) }.map { GroupTile(it) } + AddTile("group")
            t == "apps" -> i.user.apps.filter { ok(it.label) }.map { AppTile(it) } + AddTile("app")
            t == "fav" -> gameTiles(i.user.favorites.mapNotNull { byId[it] }.sortedBy { it.title.lowercase() })
            t == "recent" -> gameTiles(i.user.recents.mapNotNull { byId[it] })
            t.startsWith("sys:") -> gameTiles(sorted(i.games.filter { it.systemId == t.removePrefix("sys:") }))
            t.startsWith("group:") -> {
                val g = i.user.groups.firstOrNull { it.id == t.removePrefix("group:") }
                gameTiles(g?.gameIds.orEmpty().mapNotNull { byId[it] })
            }
            else -> i.user.groups.filter { q.isEmpty() }.map { GroupTile(it) } + gameTiles(sorted(i.games))
        }
    }

    val selected: StateFlow<Tile?> = combine(tiles, selectedKey) { list, k ->
        list.firstOrNull { it.key == k } ?: list.firstOrNull { it !is AddTile }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun select(t: Tile) { selectedKey.value = t.key }

    fun cycleLayout() {
        val next = when (settings.value.layout) {
            LibraryLayout.PAGES -> LibraryLayout.GRID
            LibraryLayout.GRID -> LibraryLayout.CAROUSEL
            LibraryLayout.CAROUSEL -> LibraryLayout.PAGES
        }
        viewModelScope.launch { prefs.setLayout(next) }
    }
    fun jumpToGame(g: Game) { tab.value = "all"; query.value = ""; selectedKey.value = "g:${g.id}" }

    fun activate(t: Tile) {
        when (t) {
            is GameTile -> launchGame(t.game)
            is GroupTile -> { tab.value = "group:${t.group.id}"; selectedKey.value = null }
            is AppTile -> Launcher.launchApp(ctx, t.app.pkg)
            is AddTile -> show(if (t.kind == "group") Dlg.NewGroup() else Dlg.AddApp)
        }
    }

    fun longPress(t: Tile) {
        when (t) {
            is GameTile -> show(Dlg.GameMenu(t.game))
            is GroupTile -> show(Dlg.GroupMenu(t.group))
            is AppTile -> show(Dlg.AppMenu(t.app))
            is AddTile -> Unit
        }
    }

    private fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /**
     * Launches a game and awards Play Coins: 1 per launch (up to 10 a day), +3 the first time you
     * ever play a particular game. The wallet holds 300 at most, like the handheld it's inspired by.
     */
    fun launchGame(game: Game, chooser: Boolean = false) {
        var gain = 0
        updateUser { u ->
            val day = today()
            val doneToday = if (u.coinDay == day) u.coinsToday else 0
            var g = 0
            if (doneToday < 10) g += 1
            if (u.stats[game.id] == null) g += 3
            val total = (u.coins + g).coerceAtMost(300)
            gain = total - u.coins
            val st = u.stats[game.id] ?: GameStat()
            u.copy(
                recents = (listOf(game.id) + u.recents.filter { x -> x != game.id }).take(24),
                coins = total, coinDay = day,
                coinsToday = if (doneToday < 10) doneToday + 1 else doneToday,
                stats = u.stats + (game.id to GameStat(st.plays + 1, System.currentTimeMillis())),
            )
        }
        if (gain > 0) Toast.makeText(ctx, "+$gain Play Coin${if (gain == 1) "" else "s"}!", Toast.LENGTH_SHORT).show()
        Launcher.launchGame(ctx, game, _user.value, chooser)
    }

    fun randomGame() {
        val g = tiles.value.filterIsInstance<GameTile>().randomOrNull() ?: return
        selectedKey.value = g.key
    }

    /** Spends Play Coins on a store theme. Returns false if you can't afford it. */
    fun buyTheme(t: ThemeDef): Boolean {
        if (t.cost > _user.value.coins) return false
        updateUser { it.copy(coins = it.coins - t.cost, owned = it.owned + t.id) }
        installTheme(t)
        applyTheme(t.id)
        return true
    }

    // ------------------------------------------------------------ ROM folders
    fun addRomFolder(uri: Uri) {
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val s = uri.toString()
        viewModelScope.launch { prefs.setRoms((settings.value.romsUris + s).distinct()) }
    }

    fun removeRomFolder(uri: String) {
        viewModelScope.launch { prefs.setRoms(settings.value.romsUris - uri) }
    }

    fun rescan() {
        val uris = settings.value.romsUris
        viewModelScope.launch {
            if (uris.isEmpty()) {
                _games.value = emptyList(); RomScanner.saveCache(ctx, emptyList()); return@launch
            }
            _scanning.value = true
            scanCount.value = 0
            val found = ArrayList<Game>()
            var failed = false
            for (u in uris) {
                runCatching { found += RomScanner.scan(ctx, Uri.parse(u)) { scanCount.value = found.size + it } }
                    .onFailure { failed = true }
            }
            val result = found.distinctBy { it.id }.sortedBy { it.title.lowercase() }
            if (result.isNotEmpty() || !failed) {
                _games.value = result
                RomScanner.saveCache(ctx, result)
            }
            _scanning.value = false
            fetchArt()
        }
    }

    // ------------------------------------------------------------ art
    private var artJob: Job? = null

    private fun fetchArt() {
        artJob?.cancel()
        val key = settings.value.sgdbKey
        artJob = viewModelScope.launch {
            autoArt.value = _games.value.mapNotNull { g -> sgdb.cached(g)?.let { g.id to it } }.toMap()
            if (key.isBlank()) return@launch
            val ordered = (tiles.value.filterIsInstance<GameTile>().map { it.game } + _games.value).distinctBy { it.id }
            for (g in ordered) {
                if (sgdb.hasLookedUp(g) || _user.value.customArt.containsKey(g.id)) continue
                sgdb.fetchCover(g, key)?.let { url -> autoArt.update { it + (g.id to url) } }
            }
        }
    }

    fun setArt(gameId: String, url: String?) = updateUser {
        it.copy(customArt = if (url == null) it.customArt - gameId else it.customArt + (gameId to url))
    }

    /** Copies a picked image into app storage so it survives the original being moved. */
    fun importArt(gameId: String, uri: Uri) {
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) {
                val dir = File(ctx.filesDir, "art").apply { mkdirs() }
                val out = File(dir, "${gameId.hashCode().toUInt()}_${System.currentTimeMillis()}.img")
                ctx.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                out.takeIf { it.length() > 0 }
            }
            if (f != null) setArt(gameId, Uri.fromFile(f).toString())
        }
    }

    fun importWallpaper(uri: Uri) {
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) {
                val out = File(ctx.filesDir, "wallpaper_${System.currentTimeMillis()}.img")
                ctx.contentResolver.openInputStream(uri)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                out.takeIf { it.length() > 0 }
            }
            if (f != null) prefs.setWallpaper(Uri.fromFile(f).toString())
        }
    }

    // ------------------------------------------------------------ favourites / groups / apps
    fun toggleFavorite(id: String) = updateUser {
        it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id)
    }

    fun createGroup(name: String, colorIdx: Int, addGameId: String? = null) = updateUser {
        val id = "g" + System.currentTimeMillis().toString(36)
        it.copy(groups = it.groups + Group(id, name.trim().ifEmpty { "New group" }, colorIdx, listOfNotNull(addGameId)))
    }

    fun renameGroup(id: String, name: String, colorIdx: Int) = updateUser { u ->
        u.copy(groups = u.groups.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }, colorIdx = colorIdx) else it })
    }

    fun deleteGroup(id: String) {
        if (tab.value == "group:$id") tab.value = "groups"
        updateUser { u -> u.copy(groups = u.groups.filter { it.id != id }) }
    }

    fun toggleInGroup(groupId: String, gameId: String) = updateUser { u ->
        u.copy(groups = u.groups.map {
            if (it.id != groupId) it
            else it.copy(gameIds = if (gameId in it.gameIds) it.gameIds - gameId else it.gameIds + gameId)
        })
    }

    fun addApp(a: AppEntry) = updateUser { u -> u.copy(apps = (u.apps.filter { it.pkg != a.pkg } + a)) }
    fun removeApp(pkg: String) = updateUser { u -> u.copy(apps = u.apps.filter { it.pkg != pkg }) }

    suspend fun launchableApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    // ------------------------------------------------------------ emulator profiles
    fun setProfile(key: String, p: LaunchProfile?) = updateUser {
        it.copy(profiles = if (p == null) it.profiles - key else it.profiles + (key to p))
    }

    // ------------------------------------------------------------ avatar
    fun setAvatar(a: Avatar) = updateUser { it.copy(avatar = a) }

    // ------------------------------------------------------------ themes
    val allThemes: StateFlow<List<ThemeDef>> = _user.map { Themes.builtin + it.themes }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Themes.builtin)

    val activeTheme: StateFlow<ThemeDef> = combine(allThemes, settings) { list, s ->
        list.firstOrNull { it.id == s.themeId } ?: Themes.builtin.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Themes.builtin.first())

    val storeThemes = MutableStateFlow<List<ThemeDef>>(emptyList())
    val storeStatus = MutableStateFlow("")

    fun fetchStore() {
        viewModelScope.launch {
            storeStatus.value = "Loading themes…"
            Themes.fetchStore()
                .onSuccess { storeThemes.value = it; storeStatus.value = if (it.isEmpty()) "No online themes yet." else "" }
                .onFailure { storeStatus.value = "Couldn't reach the Theme Store. Check your connection." }
        }
    }

    fun installTheme(t: ThemeDef) = updateUser { u -> u.copy(themes = u.themes.filter { it.id != t.id } + t.copy(builtin = false)) }

    fun removeTheme(id: String) {
        if (settings.value.themeId == id) viewModelScope.launch { prefs.setThemeId("home") }
        updateUser { u -> u.copy(themes = u.themes.filter { it.id != id }) }
    }

    fun applyTheme(id: String) { viewModelScope.launch { prefs.setThemeId(id) } }

    /** Returns how many themes were imported from pasted/picked JSON. */
    fun importThemes(text: String): Int {
        val list = Themes.parseImport(text).filter { t -> Themes.builtin.none { it.id == t.id } }
        list.forEach { installTheme(it) }
        return list.size
    }

    // ------------------------------------------------------------ RetroAchievements
    private val _profile = MutableStateFlow<RaProfile?>(null)
    val profile: StateFlow<RaProfile?> = _profile
    private val _progress = MutableStateFlow<Map<String, RaProgress>>(emptyMap())
    val progress: StateFlow<Map<String, RaProgress>> = _progress
    val raStatus = MutableStateFlow("")

    fun refreshRa() {
        val s = settings.value
        viewModelScope.launch {
            if (s.raUser.isBlank() || s.raKey.isBlank()) {
                _profile.value = null; _progress.value = emptyMap(); raStatus.value = ""
                return@launch
            }
            raStatus.value = "Connecting…"
            val p = RetroAchievements.profile(s.raUser, s.raKey)
            _profile.value = p
            raStatus.value = if (p == null) "Couldn't connect. Check username and Web API key." else "Connected as ${p.user}"
            if (p != null) _progress.value = RetroAchievements.progress(s.raUser, s.raKey)
        }
    }

    init {
        // Rescan whenever the ROM folder list changes.
        viewModelScope.launch {
            var first = true
            settings.map { it.romsUris }.distinctUntilChanged().collect { list ->
                // The very first value is the empty default, before saved settings load: don't wipe the cache.
                if (!(first && list.isEmpty())) rescan()
                first = false
            }
        }
        viewModelScope.launch {
            settings.map { it.raUser to it.raKey }.distinctUntilChanged().collect { refreshRa() }
        }
        viewModelScope.launch {
            settings.map { it.sgdbKey }.distinctUntilChanged().collect { key ->
                if (key.isNotBlank()) sgdb.clearMisses()
                fetchArt()
            }
        }
    }
}
