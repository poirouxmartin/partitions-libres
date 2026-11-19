package com.musique.partitions.data

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class LibraryStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(tmp.root, "library.json")
    }

    @Test
    fun emptyWhenNoFile() {
        assertEquals(Library(), LibraryStore(file).load())
    }

    @Test
    fun togglesFavouritesAndPersists() {
        val store = LibraryStore(file)
        store.toggleFavourite("a")
        assertEquals(listOf("b", "a"), store.toggleFavourite("b").favourites)
        assertEquals(listOf("b"), store.toggleFavourite("a").favourites)
        assertEquals(listOf("b"), LibraryStore(file).load().favourites)
    }

    @Test
    fun recentsKeepLastOpenedWithoutDuplicatesAndCap() {
        val store = LibraryStore(file, maxRecents = 3)
        listOf("a", "b", "c", "a", "d").forEach { store.markOpened(it) }
        assertEquals(listOf("d", "a", "c"), LibraryStore(file, maxRecents = 3).load().recents)
    }

    @Test
    fun corruptFileGivesEmptyLibrary() {
        file.writeText("{not json")
        assertEquals(Library(), LibraryStore(file).load())
    }
}
