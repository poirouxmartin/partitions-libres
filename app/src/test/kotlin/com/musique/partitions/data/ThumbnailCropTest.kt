package com.musique.partitions.data

import org.junit.Test
import kotlin.test.assertEquals

class ThumbnailCropTest {
    @Test
    fun startsJustAboveFirstInk() {
        val rows = BooleanArray(1200).also { it[100] = true }
        assertEquals(100 - 900 / 40 until 100 - 900 / 40 + 405, ThumbnailCrop.range(rows, 900))
    }

    @Test
    fun noInkStartsAtTop() {
        assertEquals(0 until 405, ThumbnailCrop.range(BooleanArray(1200), 900))
    }

    @Test
    fun inkNearTopDoesNotGoNegative() {
        val rows = BooleanArray(1200).also { it[5] = true }
        assertEquals(0 until 405, ThumbnailCrop.range(rows, 900))
    }

    @Test
    fun shortPageIsClamped() {
        val rows = BooleanArray(300).also { it[50] = true }
        val r = ThumbnailCrop.range(rows, 900)
        assertEquals(28, r.first)
        assertEquals(299, r.last)
    }
}
