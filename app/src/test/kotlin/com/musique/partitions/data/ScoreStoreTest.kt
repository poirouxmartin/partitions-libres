package com.musique.partitions.data

import com.musique.partitions.catalog.Piece
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var saved: File
    private lateinit var cache: File
    private var fetches = 0
    private var payload = ByteArray(100_000) { (it * 31).toByte() }
    private var failMidway = false

    private val fetcher = Fetcher { _ ->
        fetches++
        if (failMidway) {
            object : InputStream() {
                var n = 0
                override fun read(): Int = if (n++ < 1000) 1 else throw IOException("coupure")
            }
        } else {
            ByteArrayInputStream(payload)
        }
    }

    private lateinit var store: ScoreStore

    private fun piece(id: String = "p1", pdf: Boolean = false) = Piece(
        id = id, title = "Titre $id", composer = "Anon", composerSort = "Anon",
        licence = "CC0-1.0", licenceLabel = "CC0", licenceUrl = "https://example.org/l",
        engraver = "Moi", source = "Src",
        musicxmlUrl = if (pdf) null else "https://example.org/$id.mxl",
        pdfUrl = if (pdf) "https://example.org/$id.pdf" else null,
    )

    @Before
    fun setUp() {
        saved = File(tmp.root, "saved")
        cache = File(tmp.root, "cache")
        store = ScoreStore(saved, cache, fetcher)
    }

    @Test
    fun saveWritesFileAndSidecarByteIdentical() = runTest {
        val p = piece()
        val progress = mutableListOf<Float>()
        store.save(p) { progress += it }
        assertTrue(store.isSaved("p1"))
        assertContentEquals(payload, File(saved, "p1.mxl").readBytes())
        assertTrue(File(saved, "p1.json").exists())
        assertTrue(progress.isNotEmpty())
        assertEquals(listOf(p), store.savedPieces())
    }

    @Test
    fun savedPiecesReadsSidecarsSorted() = runTest {
        store.save(piece("b", pdf = true)) {}
        store.save(piece("a")) {}
        assertEquals(listOf("a", "b"), store.savedPieces().map { it.id })
        assertEquals("https://example.org/b.pdf", store.savedPieces().last().pdfUrl)
    }

    @Test
    fun fileForViewingPrefersSavedThenCache() = runTest {
        val p = piece()
        val cached = store.fileForViewing(p)
        assertEquals(cache, cached.parentFile)
        assertEquals(1, fetches)
        store.fileForViewing(p)
        assertEquals(1, fetches) // reuses the cache
        store.save(p) {}
        assertEquals(2, fetches)
        val viewed = store.fileForViewing(p)
        assertEquals(saved, viewed.parentFile)
        assertEquals(2, fetches)
    }

    @Test
    fun deleteRemovesFileAndSidecar() = runTest {
        store.save(piece()) {}
        store.delete("p1")
        assertFalse(store.isSaved("p1"))
        assertFalse(File(saved, "p1.mxl").exists())
        assertFalse(File(saved, "p1.json").exists())
        assertTrue(store.savedPieces().isEmpty())
    }

    @Test
    fun failedDownloadLeavesNoTmpNorPartialFile() = runTest {
        failMidway = true
        assertFailsWith<IOException> { store.save(piece()) {} }
        assertFalse(store.isSaved("p1"))
        assertTrue(saved.listFiles().orEmpty().isEmpty())
        assertFailsWith<IOException> { store.fileForViewing(piece()) }
        assertTrue(cache.listFiles().orEmpty().isEmpty())
    }
}
