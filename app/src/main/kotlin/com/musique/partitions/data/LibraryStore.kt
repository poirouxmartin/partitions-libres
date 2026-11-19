package com.musique.partitions.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Favourites and recently opened scores, by piece id (most recent first). */
@Serializable
data class Library(val favourites: List<String> = emptyList(), val recents: List<String> = emptyList())

/**
 * Persists [Library] in [file]. Missing or unreadable file: empty library.
 * All operations are synchronized: reads and writes never overlap.
 */
class LibraryStore(private val file: File, private val maxRecents: Int = 20) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: Library? = null

    @Synchronized
    fun load(): Library = cached ?: readFile().also { cached = it }

    /** Adds [id] to the top of the favourites, or removes it if already there. */
    @Synchronized
    fun toggleFavourite(id: String): Library {
        val lib = load()
        val favourites = if (id in lib.favourites) lib.favourites - id else listOf(id) + lib.favourites
        return save(lib.copy(favourites = favourites))
    }

    /** Moves [id] to the top of the recents, without duplicates, keeping at most [maxRecents] entries. */
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
