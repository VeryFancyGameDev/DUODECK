package com.duodeck.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duodeck.launcher.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val sgdb = SteamGridDb(app)

    val settings = prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _games = MutableStateFlow(RomScanner.loadCache(app))
    val games: StateFlow<List<Game>> = _games

    private val _art = MutableStateFlow<Map<String, String>>(emptyMap())
    val art: StateFlow<Map<String, String>> = _art

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning
    val scanCount = MutableStateFlow(0)

    val systemFilter = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")
    val selectedId = MutableStateFlow<String?>(null)
    val showSettings = MutableStateFlow(false)

    private val _profile = MutableStateFlow<RaProfile?>(null)
    val profile: StateFlow<RaProfile?> = _profile
    private val _progress = MutableStateFlow<Map<String, RaProgress>>(emptyMap())
    val progress: StateFlow<Map<String, RaProgress>> = _progress
    val raStatus = MutableStateFlow("")

    /** Games after system filter + search (+ optional "has art" filter). */
    val visible: StateFlow<List<Game>> = combine(games, systemFilter, query, art, settings) { g, sys, q, a, s ->
        g.filter { (sys == null || it.systemId == sys) &&
            (q.isBlank() || it.title.contains(q, ignoreCase = true)) &&
            (!s.hideNoArt || a.containsKey(it.id)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Systems that actually have games, in a stable order. */
    val systemsPresent: StateFlow<List<GameSystem>> = games.map { g ->
        val ids = g.map { it.systemId }.toSet()
        Systems.all.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selected: StateFlow<Game?> = combine(visible, selectedId) { v, id ->
        v.firstOrNull { it.id == id } ?: v.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var artJob: Job? = null

    init {
        // Load cached art immediately.
        viewModelScope.launch {
            _art.value = _games.value.mapNotNull { g -> sgdb.cached(g)?.let { g.id to it } }.toMap()
        }
        // Rescan whenever the ROM folder changes; refresh RA when credentials change.
        viewModelScope.launch {
            settings.map { it.romsUri }.distinctUntilChanged().collect { uri ->
                if (uri != null) rescan()
            }
        }
        viewModelScope.launch {
            settings.map { Triple(it.raUser, it.raKey, 0) }.distinctUntilChanged().collect { refreshRa() }
        }
        viewModelScope.launch {
            settings.map { it.sgdbKey }.distinctUntilChanged().collect { key ->
                if (key.isNotBlank()) { sgdb.clearMisses(); fetchArt() }
            }
        }
    }

    fun select(game: Game) { selectedId.value = game.id }

    fun toggleLayout() {
        viewModelScope.launch {
            prefs.setLayout(if (settings.value.layout == LibraryLayout.GRID) LibraryLayout.CAROUSEL else LibraryLayout.GRID)
        }
    }

    fun pickRomFolder(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModelScope.launch { prefs.setRoms(uri.toString()) }
    }

    fun rescan() {
        val uri = settings.value.romsUri?.let(Uri::parse) ?: return
        viewModelScope.launch {
            _scanning.value = true
            scanCount.value = 0
            val found = runCatching {
                RomScanner.scan(getApplication(), uri) { scanCount.value = it }
            }.getOrDefault(emptyList())
            if (found.isNotEmpty() || _games.value.isEmpty()) {
                _games.value = found
                RomScanner.saveCache(getApplication(), found)
            }
            _scanning.value = false
            fetchArt()
        }
    }

    private fun fetchArt() {
        artJob?.cancel()
        val key = settings.value.sgdbKey
        artJob = viewModelScope.launch {
            _art.value = _games.value.mapNotNull { g -> sgdb.cached(g)?.let { g.id to it } }.toMap()
            if (key.isBlank()) return@launch
            // Selected/visible games first, so the screen fills in as you look at it.
            val ordered = (visible.value + _games.value).distinctBy { it.id }
            for (g in ordered) {
                if (sgdb.hasLookedUp(g)) continue
                sgdb.fetchCover(g, key)?.let { url -> _art.update { it + (g.id to url) } }
            }
        }
    }

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

    fun progressFor(game: Game): RaProgress? =
        _progress.value[RetroAchievements.normalise(game.title)]
}
