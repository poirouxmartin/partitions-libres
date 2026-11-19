package com.musique.partitions.data

object ThumbnailCrop {
    /**
     * Rows of the page top to keep: title and first system. [inkRows][y] is true when row y has
     * non-white pixels; the crop starts just above the first one and is 9/20 of [width] tall.
     */
    fun range(inkRows: BooleanArray, width: Int): IntRange {
        val first = inkRows.indexOfFirst { it }
        val start = if (first < 0) 0 else maxOf(0, first - width / 40)
        val height = minOf(width * 9 / 20, inkRows.size - start)
        return start until start + height
    }
}
