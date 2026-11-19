package com.musique.render

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Picture
import android.graphics.Typeface
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGExternalFileResolver
import java.util.concurrent.ConcurrentHashMap

object SvgPages {
    @Volatile
    private var fontsInstalled = false

    /**
     * Draws Verovio's "Times" text with Liberation Serif (same metrics as Times, SIL OFL 1.1),
     * so lyrics keep the spacing Verovio computed. Call once, before the first render.
     */
    fun installTextFonts(context: Context) {
        if (fontsInstalled) return
        synchronized(this) {
            if (fontsInstalled) return
            SVG.registerExternalFileResolver(TimesFontResolver(context.applicationContext.assets))
            fontsInstalled = true
        }
    }

    /** Draws the SVG into a Picture sized from the document's own width/height. */
    fun toPicture(svg: String): Picture = SVG.getFromString(SvgFixes.prepare(svg)).renderToPicture()

    /** width / height of the SVG document, or 1 if unknown. */
    fun aspectRatio(svg: String): Float {
        val s = SVG.getFromString(svg)
        val w = s.documentWidth
        val h = s.documentHeight
        return if (w > 0f && h > 0f) w / h else 1f
    }

    private class TimesFontResolver(private val assets: AssetManager) : SVGExternalFileResolver() {
        private val faces = ConcurrentHashMap<String, Typeface>()

        override fun resolveFont(fontFamily: String?, fontWeight: Int, fontStyle: String?): Typeface? {
            if (fontFamily == null || !SvgFixes.isTimesFamily(fontFamily)) return null
            val asset = SvgFixes.timesFaceAsset(fontWeight, fontStyle)
            return faces[asset] ?: runCatching { Typeface.createFromAsset(assets, asset) }.getOrNull()?.also { faces[asset] = it }
        }
    }
}
