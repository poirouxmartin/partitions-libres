package com.musique.render

import android.content.Context

/**
 * Renders MusicXML/MEI to one SVG string per page, reflowed to a given pixel width.
 * Holds a single [Verovio] toolkit: NOT thread-safe, use it from one single-thread dispatcher.
 */
class ScoreRenderer(context: Context) : AutoCloseable {
    private val verovio = Verovio(VerovioResources.ensureUnpacked(context).absolutePath)

    val version: String get() = verovio.version

    /**
     * @param widthPx target width of the rendered pages, in pixels
     * @param zoom 1 = default staff size, >1 enlarges the music (and so lays out fewer measures per line)
     */
    fun render(data: String, widthPx: Int, zoom: Float = 1f): List<String> {
        // Verovio's pageWidth is in internal units, and the SVG is drawn at pageWidth * scale / 100 px.
        // To get pages exactly widthPx wide we therefore need pageWidth = widthPx * 100 / scale.
        val scale = (BASE_SCALE * zoom).toInt().coerceIn(10, 400)
        val pageWidth = widthPx * 100 / scale
        val options = """
            {"scale": $scale, "pageWidth": $pageWidth, "pageHeight": 60000, "adjustPageHeight": true,
             "breaks": "auto", "footer": "none", "header": "auto",
             "pageMarginLeft": 40, "pageMarginRight": 40, "pageMarginTop": 40, "pageMarginBottom": 40}
        """.trimIndent()
        check(verovio.setOptions(options)) { "Verovio rejected the layout options" }
        check(verovio.loadData(data)) { "Verovio could not load the score" }
        return (1..verovio.pageCount()).map { verovio.renderToSvg(it) }
    }

    override fun close() = verovio.close()

    private companion object {
        const val BASE_SCALE = 40
    }
}
