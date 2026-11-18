package com.musique.partitions.catalog

import com.musique.partitions.catalog.InstrumentFamily.GUITAR
import com.musique.partitions.catalog.InstrumentFamily.ORGAN
import com.musique.partitions.catalog.InstrumentFamily.PIANO
import com.musique.partitions.catalog.InstrumentFamily.STRINGS
import com.musique.partitions.catalog.InstrumentFamily.VOICE
import com.musique.partitions.catalog.InstrumentFamily.WINDS
import kotlin.test.Test
import kotlin.test.assertEquals

class InstrumentFamilyTest {
    private fun of(s: String) = InstrumentFamily.of(s)

    @Test
    fun commonCatalogValues() {
        assertEquals(setOf(VOICE, PIANO), of("Voix, piano"))
        assertEquals(setOf(PIANO), of("Piano"))
        assertEquals(setOf(STRINGS), of("Quatuor à cordes"))
        assertEquals(setOf(ORGAN), of("Organ"))
        assertEquals(setOf(VOICE), of("Voice (SATB)"))
        assertEquals(setOf(GUITAR), of("Classical Guitar"))
        assertEquals(setOf(STRINGS), of("String Quartet: Two Violins, Viola, 'Cello"))
        assertEquals(setOf(VOICE, PIANO), of("Soprano, Alto, Tenor, Bass, Piano"))
    }

    @Test
    fun ambiguousShortWordsNeedWholeWord() {
        assertEquals(setOf(PIANO), of("Harpsichord, Clavichord"))
        assertEquals(setOf(STRINGS), of("Harp"))
        assertEquals(setOf(WINDS), of("Horn Duet"))
        assertEquals(setOf(WINDS), of("Cor anglais"))
        assertEquals(emptySet(), of("Hornpipe"))
    }

    @Test
    fun unknownOrEmpty() {
        assertEquals(emptySet(), of(""))
        assertEquals(emptySet(), of("Koto"))
        assertEquals(setOf(WINDS), of("Flûte"))
    }
}
