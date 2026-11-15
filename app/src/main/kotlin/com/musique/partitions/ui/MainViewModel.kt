package com.musique.partitions.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musique.partitions.PartitionsApp
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.data.LoadedCatalog
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

    /** Progression des téléchargements en cours (0..1, négatif si taille inconnue). */
    val downloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadError = MutableStateFlow<String?>(null)

    val update = MutableStateFlow<ReleaseInfo?>(null)
    val installing = MutableStateFlow(false)

    val results: StateFlow<List<Piece>> = combine(query.debounce(150), catalog) { q, c -> q to c }
        .mapLatest { (q, c) ->
            if (q.isBlank() || c !is CatalogState.Ready) emptyList() else c.data.search.search(q)
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
        refreshSaved()
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
        intent.getStringExtra("view")?.let { stack.value = listOf(Screen.Home, Screen.Viewer(it)) }
    }

    fun push(screen: Screen) = stack.update { it + screen }
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
            try {
                app.updateInstaller.install(info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Silencieux : la bannière reste, l'utilisateur peut réessayer.
            } finally {
                installing.value = false
            }
        }
    }

    fun dismissUpdate(info: ReleaseInfo) {
        app.updateChecker.dismiss(info.tag)
        update.value = null
    }
}
