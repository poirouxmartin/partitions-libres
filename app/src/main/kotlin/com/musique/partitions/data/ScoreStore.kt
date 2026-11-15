package com.musique.partitions.data

import com.musique.partitions.catalog.Piece
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Partitions enregistrées dans [savedDir] (`<id>.<ext>` + fiche `<id>.json` pour l'attribution hors ligne)
 * et copies temporaires de lecture dans [cacheDir]. Les fichiers sont conservés tels que téléchargés
 * (CC BY-SA : aucune modification).
 */
class ScoreStore(
    private val savedDir: File,
    private val cacheDir: File,
    private val fetcher: Fetcher,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun savedFile(piece: Piece) = File(savedDir, "${piece.id}.${piece.fileExtension}")
    private fun cachedFile(piece: Piece) = File(cacheDir, "${piece.id}.${piece.fileExtension}")
    private fun sidecar(id: String) = File(savedDir, "$id.json")

    /** La fiche est écrite en dernier : sa présence implique que le fichier est complet. */
    fun isSaved(id: String): Boolean = sidecar(id).exists()

    fun savedPieces(): List<Piece> =
        savedDir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { json.decodeFromString<Piece>(it.readText()) }.getOrNull() }
            .filter { savedFile(it).exists() }
            .sortedWith(compareBy({ it.composerSort }, { it.title }))

    suspend fun save(piece: Piece, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        download(fetcher, piece.fileUrl, savedFile(piece), onProgress)
        try {
            writeAtomically(sidecar(piece.id)) { it.write(json.encodeToString(piece).toByteArray()) }
        } catch (e: Exception) {
            savedFile(piece).delete()
            throw e
        }
    }

    suspend fun fileForViewing(piece: Piece): File = withContext(Dispatchers.IO) {
        val saved = savedFile(piece)
        if (saved.exists()) return@withContext saved
        val cached = cachedFile(piece)
        if (!cached.exists()) download(fetcher, piece.fileUrl, cached)
        cached
    }

    fun delete(id: String) {
        sidecar(id).delete()
        for (ext in listOf("mxl", "pdf")) File(savedDir, "$id.$ext").delete()
    }
}
