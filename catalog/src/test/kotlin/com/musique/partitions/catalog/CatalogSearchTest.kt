package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogSearchTest {
    private fun p(id: String, title: String, composer: String, sort: String, instruments: String = "Piano") = Piece(
        id = id, title = title, composer = composer, composerSort = sort, instruments = instruments,
        licence = "PD", licenceLabel = "PD", licenceUrl = "u", engraver = "e", source = "s",
        pdfUrl = "http://x/$id.pdf",
    )

    private val search = CatalogSearch(
        listOf(
            p("r1", "Prelude Op. 23, No. 1", "S. Rachmaninoff", "Rachmaninoff, S"),
            p("r2", "Prelude in C# minor", "S. Rachmaninoff", "Rachmaninoff, S"),
            p("s1", "Erlkönig", "Franz Schubert", "Schubert, Franz", "Voice, Piano"),
            p("s2", "Die schöne Müllerin", "Franz Schubert", "Schubert, Franz", "Voice, Piano"),
            p("s3", "Ave Maria", "Franz Schubert", "Schubert, Franz", "Voice, Piano"),
            p("u1", "Kinderszenen", "Robert Schumann", "Schumann, Robert"),
            p("b1", "Invention 1", "J. S. Bach", "Bach, Johann Sebastian", "Keyboard"),
            p("h1", "Sonata", "Joseph Haydn", "Haydn, Joseph"),
        ),
    )

    private fun ids(q: String) = search.search(q).map { it.id }

    @Test fun composerVariant() = assertEquals(setOf("r1", "r2"), ids("Rachmaninov").toSet())
    @Test fun composerAndTitle() = assertEquals(setOf("r1", "r2"), ids("rachmaninov prelude").toSet())
    @Test fun opusNumber() = assertEquals(listOf("r1"), ids("prelude op 23"))
    @Test fun schubertExcludesSchumann() {
        val r = ids("schubert")
        assertEquals(setOf("s1", "s2", "s3"), r.toSet())
        assertTrue("u1" !in r)
    }
    @Test fun accentFolding() = assertTrue("s1" in ids("erlkonig"))
    @Test fun blank() = assertEquals(emptyList(), ids("   "))
    @Test fun typo() = assertTrue("s1" in ids("erlkonng"))

    @Test
    fun composersGrouped() {
        val c = search.composers()
        assertEquals(5, c.size)
        assertEquals("Bach, Johann Sebastian", c.first().sort)
        assertEquals(3, c.first { it.sort.startsWith("Schubert") }.count)
    }
}
