package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogParserTest {
    private fun piece(id: String, licence: String, extra: String) = """
        {"id":"$id","title":"T $id","composer":"C","composerSort":"C","licence":"$licence",
         "licenceLabel":"l","licenceUrl":"u","engraver":"e","source":"s","unknownField":42 $extra}
    """.trimIndent()

    private val sample = """
        {"schema":1,"generated":"2026-01-01","extra":true,"pieces":[
          ${piece("a", "PD", ",\"musicxmlUrl\":\"http://x/a.mxl\",\"born\":1873,\"died\":1943")},
          ${piece("b", "CC-BY-SA-4.0", ",\"pdfUrl\":\"http://x/b.pdf\"")},
          ${piece("c", "CC-BY-NC-4.0", ",\"pdfUrl\":\"http://x/c.pdf\"")},
          ${piece("d", "CC0", "")}
        ]}
    """.trimIndent()

    @Test
    fun dropsNonCommercialAndUrlless() {
        val c = CatalogParser.parse(sample)
        assertEquals(listOf("a", "b"), c.pieces.map { it.id })
        assertEquals(1, c.schema)
    }

    @Test
    fun computedMembers() {
        val (a, b) = CatalogParser.parse(sample).pieces
        assertEquals(ScoreFormat.MUSICXML, a.format)
        assertEquals("mxl", a.fileExtension)
        assertEquals("1873–1943", a.lifeDates)
        assertEquals(ScoreFormat.PDF, b.format)
        assertEquals("pdf", b.fileExtension)
        assertEquals("", b.lifeDates)
        assertTrue(b.isShareAlike)
    }
}
