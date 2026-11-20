package com.musique.partitions.ui

import org.junit.Test
import kotlin.test.assertEquals

class ViewerZoomTest {
    @Test
    fun clampKeepsZoomInRangeAndSnapsNearOne() {
        assertEquals(1f, ViewerZoom.clamp(0.4f))
        assertEquals(1f, ViewerZoom.clamp(1.05f))
        assertEquals(1.5f, ViewerZoom.clamp(1.5f))
        assertEquals(3f, ViewerZoom.clamp(7f))
    }

    @Test
    fun focusPointStaysUnderTheFingers() {
        // Content point under the focus: 100 + 50 = 150; doubled it lies at 300, still 50 px into the viewport.
        assertEquals(250f, ViewerZoom.scrollAfter(scroll = 100f, focus = 50f, factor = 2f))
        // Zooming back out to the top-left corner.
        assertEquals(50f, ViewerZoom.scrollAfter(scroll = 100f, focus = 0f, factor = 0.5f))
        // Never a negative scroll.
        assertEquals(0f, ViewerZoom.scrollAfter(scroll = 0f, focus = 300f, factor = 0.5f))
    }

    @Test
    fun pdfRasterIsCapped() {
        assertEquals(1000, ViewerZoom.pdfRenderWidth(1000))
        assertEquals(ViewerZoom.MAX_PDF_WIDTH, ViewerZoom.pdfRenderWidth(5000))
    }

    @Test
    fun labelIsShort() {
        assertEquals("×1.5", ViewerZoom.label(1.52f))
        assertEquals("×2", ViewerZoom.label(2.01f))
    }
}
