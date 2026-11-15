package com.musique.render

import android.graphics.Picture
import com.caverock.androidsvg.SVG

object SvgPages {
    /** Draws the SVG into a Picture sized from the document's own width/height. */
    fun toPicture(svg: String): Picture = SVG.getFromString(svg).renderToPicture()

    /** width / height of the SVG document, or 1 if unknown. */
    fun aspectRatio(svg: String): Float {
        val s = SVG.getFromString(svg)
        val w = s.documentWidth
        val h = s.documentHeight
        return if (w > 0f && h > 0f) w / h else 1f
    }
}
