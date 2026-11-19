package com.musique.partitions.catalog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class CatalogLoadTimeTest {
    private fun ms(from: Long) = (System.nanoTime() - from) / 1_000_000

    private fun run(text: String): LongArray {
        var t = System.nanoTime()
        val catalog = CatalogParser.parse(text)
        val parse = ms(t)
        t = System.nanoTime()
        val search = CatalogSearch(catalog.pieces)
        val index = ms(t)
        t = System.nanoTime()
        search.composers()
        val composers = ms(t)
        t = System.nanoTime()
        catalog.pieces.forEach { InstrumentFamily.of(it.instruments) }
        val families = ms(t)
        return longArrayOf(parse, index, composers, families)
    }

    @Test
    fun loadTimeBreakdown() {
        val file = listOf("../app/src/main/assets/catalog.json", "app/src/main/assets/catalog.json")
            .map(::File).first { it.exists() }
        val text = file.readText()
        val cold = run(text) // also the warm-up
        println("LOADTIME cold parse=${cold[0]}ms index=${cold[1]}ms composers=${cold[2]}ms families=${cold[3]}ms total=${cold.sum()}ms")
        val r = run(text)
        println("LOADTIME warm parse=${r[0]}ms index=${r[1]}ms composers=${r[2]}ms families=${r[3]}ms total=${r.sum()}ms")
        assertTrue(r.sum() < 5000, "catalog load too slow: ${r.sum()} ms")
    }
}
