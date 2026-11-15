package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TextNormalizerTest {
    private fun key(s: String) = TextNormalizer.phoneticKey(s)

    private fun same(vararg names: String) {
        val keys = names.map(::key)
        names.forEachIndexed { i, n -> assertEquals(keys[0], keys[i], "$n vs ${names[0]}") }
    }

    @Test fun rachmaninov() = same("Rachmaninov", "Rachmaninoff", "Rakhmaninov", "Rachmaninow")
    @Test fun tchaikovsky() = same("Tchaikovsky", "Tschaikowsky", "Tchaïkovski", "Chaikovsky")
    @Test fun mussorgsky() = same("Moussorgski", "Mussorgsky")
    @Test fun scriabin() = same("Scriabine", "Skrjabin", "Scriabin")
    @Test fun prokofiev() = same("Prokofiev", "Prokofieff", "Prokofjew")
    @Test fun dvorak() = same("Dvořák", "Dvorak")
    @Test fun schoenberg() = same("Schönberg", "Schoenberg")
    @Test fun stravinsky() = same("Stravinsky", "Strawinski")

    @Test
    fun differ() {
        assertNotEquals(key("Schubert"), key("Schumann"))
        assertNotEquals(key("Bach"), key("Bax"))
        assertNotEquals(key("Haydn"), key("Handel"))
    }

    @Test
    fun normalize() {
        assertEquals("erlkonig", TextNormalizer.normalize("Erlkönig"))
        assertEquals("prelude op 23 no 1", TextNormalizer.normalize("Prelude, Op. 23,  No. 1"))
        assertEquals("straße".let { "strasse" }, TextNormalizer.normalize("Straße"))
    }
}
