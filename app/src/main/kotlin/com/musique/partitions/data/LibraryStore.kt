package com.musique.partitions.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Favoris et partitions récemment ouvertes, par identifiant de pièce (le plus récent en tête). */
@Serializable
data class Library(val favourites: List<String> = emptyList(), val recents: List<String> = emptyList())

/**
 * Persiste [Library] dans [file]. Fichier absent ou illisible : bibliothèque vide.
 * Toutes les opérations sont synchronisées : la lecture et l'écriture ne se croisent pas.
 */
class LibraryStore(private val file: File, private val maxRecents: Int = 20) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: Library? = null

    @Synchronized
    fun load(): Library = cached ?: readFile().also { cached = it }

    /** Ajoute [id] en tête des favoris, ou le retire s'il y est déjà. */
    @Synchronized
    fun toggleFavourite(id: String): Library {
        val lib = load()
        val favourites = if (id in lib.favourites) lib.favourites - id else listOf(id) + lib.favourites
        return save(lib.copy(favourites = favourites))
    }

    /** Place [id] en tête des récents, sans doublon, en gardant au plus [maxRecents] entrées. */
    @Synchronized
    fun markOpened(id: String): Library {
        val lib = load()
        val recents = (listOf(id) + (lib.recents - id)).take(maxRecents)
        return save(lib.copy(recents = recents))
    }

    private fun save(lib: Library): Library {
        writeAtomically(file) { it.write(json.encodeToString(lib).toByteArray()) }
        cached = lib
        return lib
    }

    private fun readFile(): Library =
        runCatching { json.decodeFromString<Library>(file.readText()) }.getOrDefault(Library())
}
