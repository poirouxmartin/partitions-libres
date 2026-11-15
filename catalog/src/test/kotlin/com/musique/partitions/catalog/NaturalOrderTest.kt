package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class NaturalOrderTest {
    @Test
    fun numbersCompareByValue() {
        val titles = listOf("Prelude Op. 23, No. 10", "Prelude Op. 23, No. 2", "Prelude Op. 3, No. 2", "prelude op. 23, no. 1")
        assertEquals(
            listOf("Prelude Op. 3, No. 2", "prelude op. 23, no. 1", "Prelude Op. 23, No. 2", "Prelude Op. 23, No. 10"),
            titles.sortedWith(NaturalOrder),
        )
    }
}
