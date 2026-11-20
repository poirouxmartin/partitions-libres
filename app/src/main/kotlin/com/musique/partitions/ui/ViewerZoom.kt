package com.musique.partitions.ui

/** Pure arithmetic of the score viewer's pinch zoom (unit-tested on the JVM). */
internal object ViewerZoom {
    const val MIN = 1f
    const val MAX = 3f

    /** Zoom levels this close to 1 snap back to 1, so a sloppy pinch leaves no sideways scrolling. */
    const val SNAP = 1.08f

    /** Widest PDF raster: a page bitmap stays around 24 MB, beyond that the image is upscaled. */
    const val MAX_PDF_WIDTH = 2048

    fun clamp(zoom: Float): Float {
        val z = zoom.coerceIn(MIN, MAX)
        return if (z < SNAP) MIN else z
    }

    /**
     * Scroll position after the content is scaled by [factor], so that the content point under
     * [focus] (in viewport pixels) stays under it.
     */
    fun scrollAfter(scroll: Float, focus: Float, factor: Float): Float =
        ((scroll + focus) * factor - focus).coerceAtLeast(0f)

    fun pdfRenderWidth(displayWidth: Int): Int = displayWidth.coerceIn(1, MAX_PDF_WIDTH)

    /** Label of the reset chip, e.g. "×1.5". */
    fun label(zoom: Float): String = "×" + (Math.round(zoom * 10) / 10f).let {
        if (it % 1f == 0f) it.toInt().toString() else it.toString()
    }
}
