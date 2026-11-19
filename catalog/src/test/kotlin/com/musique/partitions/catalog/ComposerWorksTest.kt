package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposerWorksTest {
    private fun p(id: String, title: String, sort: String, instruments: String) = Piece(
        id = id, title = title, composer = sort, composerSort = sort, instruments = instruments,
        licence = "PD", licenceLabel = "PD", licenceUrl = "u", engraver = "e", source = "s",
        pdfUrl = "http://x/$id.pdf",
    )

    @Test
    fun groupsByFirstFamilyOrderedBySizeThenEnumThenOther() {
        val pieces = listOf(
            p("1", "Etude 10", "Bach", "Piano"),
            p("2", "Etude 2", "Bach", "Piano"),
            p("3", "Mass", "Bach", "Organ"),
            p("4", "Song", "Bach", "Voice, Piano"),
            p("5", "Mystery", "Bach", "Kazoo"),
            p("6", "Other composer", "Liszt", "Piano"),
        )
        val groups = ComposerWorks.of(pieces, "Bach")
        assertEquals(listOf(InstrumentFamily.PIANO, InstrumentFamily.VOICE, InstrumentFamily.ORGAN, null), groups.map { it.family }.let { f ->
            // Voice (1) and Organ (1) tie: enum order puts VOICE first.
            f
        })
        assertEquals(listOf("Etude 2", "Etude 10"), groups[0].pieces.map { it.title })
        assertNull(groups.last().family)
    }

    @Test
    fun matchIsExact() {
        val pieces = listOf(
            p("1", "Durch Adams Fall", "Bach, Johann Sebastian", "Organ"),
            p("2", "Prelude", "Adams, S.", "Piano"),
        )
        val groups = ComposerWorks.of(pieces, "Adams, S.")
        assertEquals(listOf("2"), groups.flatMap { it.pieces }.map { it.id })
        assertTrue(ComposerWorks.of(pieces, "Adams").isEmpty())
    }
}
