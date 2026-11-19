package com.musique.partitions.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musique.partitions.PartitionsApp
import com.musique.partitions.catalog.InstrumentFamily
import com.musique.partitions.catalog.NaturalOrder
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.data.Library
import com.musique.partitions.data.LoadedCatalog
import com.musique.partitions.data.Settings
import com.musique.partitions.update.UpdateInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Piece(val id: String) : Screen
    data class Viewer(val id: String) : Screen
    data object About : Screen
    data object Favourites : Screen
    data object Settings : Screen
}

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val data: LoadedCatalog) : CatalogState
    data class Failed(val message: String) : CatalogState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PartitionsApp

    val stack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val query = MutableStateFlow("")
    val catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val savedPieces = MutableStateFlow<List<Piece>>(emptyList())
    val library = MutableStateFlow(Library())
    val family = MutableStateFlow<InstrumentFamily?>(null)

    /** Chargés de façon synchrone : le thème doit être connu avant la première image. */
    val settings = MutableStateFlow(app.settingsStore.load())

    /** Progression des téléchargements en cours (0..1, négatif si taille inconnue). */
    val downloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadError = MutableStateFlow<String?>(null)

    /** Tourne-page demandé par une touche (pédale) : +1 avance, -1 recule. */
    val pageTurns = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 4)

    val update = MutableStateFlow<ReleaseInfo?>(null)
    val installing = MutableStateFlow(false)
    val updateMessage = MutableStateFlow<String?>(null)

    /** Sans texte ni famille : rien. Famille seule : toutes les pièces de la famille. */
    val results: StateFlow<List<Piece>> = combine(query.debounce(150), family, catalog) { q, f, c -> Triple(q, f, c) }
        .mapLatest { (q, f, c) ->
            when {
                c !is CatalogState.Ready -> emptyList()
                q.isBlank() && f == null -> emptyList()
                q.isBlank() && f != null -> c.data.catalog.pieces
                    .filter { f in c.data.families.getValue(it.id) }
                    // Parcours d'une famille : tout afficher (1 700 pièces au plus), la liste est paresseuse.
                    .sortedWith(compareBy<Piece> { it.composerSort }.thenComparator { a, b -> NaturalOrder.compare(a.title, b.title) })
                else -> c.data.search.search(q, limit = Int.MAX_VALUE)
                    .filter { f == null || f in c.data.families.getValue(it.id) }
                    .take(300)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            catalog.value = try {
                CatalogState.Ready(app.catalogRepository.get())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CatalogState.Failed(e.message ?: "erreur inconnue")
            }
        }
        viewModelScope.launch { library.value = withContext(Dispatchers.IO) { app.libraryStore.load() } }
        refreshSaved()
    }

    fun toggleFavourite(id: String) = updateLibrary { it.toggleFavourite(id) }

    fun markOpened(id: String) = updateLibrary { it.markOpened(id) }

    /** Disque plein ou autre erreur d'écriture : on garde l'état courant plutôt que de planter. */
    private fun updateLibrary(change: (com.musique.partitions.data.LibraryStore) -> com.musique.partitions.data.Library) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { change(app.libraryStore) } }.onSuccess { library.value = it }
        }
    }

    fun updateSettings(change: (Settings) -> Settings) {
        val next = change(settings.value)
        settings.value = next
        viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { app.settingsStore.save(next) } } }
    }

    /** Onglet de la barre du bas : l'accueil reste sous les autres pour que Retour y ramène. */
    fun openTab(tab: Screen) {
        stack.value = if (tab == Screen.Home) listOf(Screen.Home) else listOf(Screen.Home, tab)
    }

    fun setFamily(f: InstrumentFamily?) { family.value = f }

    fun checkUpdate() {
        viewModelScope.launch { update.value = app.updateChecker.check() }
    }

    private fun refreshSaved() {
        viewModelScope.launch { savedPieces.value = withContext(Dispatchers.IO) { app.scoreStore.savedPieces() } }
    }

    fun pieceById(id: String): Piece? =
        (catalog.value as? CatalogState.Ready)?.data?.byId?.get(id) ?: savedPieces.value.firstOrNull { it.id == id }

    fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra("query")?.let { query.value = it }
        intent.getStringExtra("open")?.let { stack.value = listOf(Screen.Home, Screen.Piece(it)) }
        when (intent.getStringExtra("tab")) {
            "favoris" -> openTab(Screen.Favourites)
            "reglages" -> openTab(Screen.Settings)
        }
        intent.getStringExtra("view")?.let { stack.value = listOf(Screen.Home, Screen.Viewer(it)); markOpened(it) }
    }

    fun push(screen: Screen) {
        if (screen is Screen.Viewer) markOpened(screen.id)
        stack.update { it + screen }
    }
    fun pop() = stack.update { if (it.size > 1) it.dropLast(1) else it }
    fun setQuery(q: String) { query.value = q }

    fun save(piece: Piece) {
        if (piece.id in downloads.value) return
        downloadError.value = null
        downloads.update { it + (piece.id to 0f) }
        viewModelScope.launch {
            try {
                app.scoreStore.save(piece) { p -> downloads.update { it + (piece.id to p) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                downloadError.value = "Téléchargement impossible. Vérifiez la connexion."
            } finally {
                downloads.update { it - piece.id }
                refreshSaved()
            }
        }
    }

    fun delete(piece: Piece) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { app.scoreStore.delete(piece.id) }
            refreshSaved()
        }
    }

    fun installUpdate(info: ReleaseInfo) {
        if (installing.value) return
        installing.value = true
        viewModelScope.launch {
            updateMessage.value = null
            try {
                val result = app.updateInstaller.install(info)
                updateMessage.value = when (result) {
                    UpdateInstaller.Result.STARTED -> null
                    UpdateInstaller.Result.NEED_PERMISSION -> "Autorisez l'installation pour cette appli, puis touchez à nouveau Installer."
                    UpdateInstaller.Result.DOWNLOAD_FAILED -> "Téléchargement impossible. Réessayez plus tard."
                    UpdateInstaller.Result.BAD_URL, UpdateInstaller.Result.BAD_APK -> "Fichier de mise à jour refusé."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateMessage.value = "Téléchargement impossible. Réessayez plus tard."
            } finally {
                installing.value = false
            }
        }
    }

    fun dismissUpdate(info: ReleaseInfo) {
        app.updateChecker.dismiss(info.tag)
        update.value = null
        updateMessage.value = null
    }
}
