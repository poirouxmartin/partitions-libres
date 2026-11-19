package com.musique.partitions.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.musique.partitions.PartitionsApp
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ScoreFormat
import com.musique.partitions.data.ThumbnailCrop
import com.musique.render.MusicXmlArchive
import com.musique.render.SvgPages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val RENDER_WIDTH = 900
private const val THUMB_WIDTH = 600

/** Cropped preview (title and first system) of the first page, or null on any failure. */
suspend fun makeThumbnail(app: PartitionsApp, piece: Piece): Bitmap? = try {
    val file = app.scoreStore.fileForViewing(piece)
    val page = when (piece.format) {
        ScoreFormat.PDF -> withContext(Dispatchers.IO) { renderPdfFirstPage(file) }
        ScoreFormat.MUSICXML -> {
            val xml = withContext(Dispatchers.IO) { MusicXmlArchive.readMusicXml(file.readBytes()) }
            withContext(app.renderDispatcher) {
                val picture = SvgPages.toPicture(app.renderer.render(xml, RENDER_WIDTH, 1f).first())
                val height = (RENDER_WIDTH.toFloat() * picture.height / picture.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(RENDER_WIDTH, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.scale(RENDER_WIDTH.toFloat() / picture.width.coerceAtLeast(1), RENDER_WIDTH.toFloat() / picture.width.coerceAtLeast(1))
                canvas.drawPicture(picture)
                bitmap
            }
        }
    }
    withContext(Dispatchers.Default) { crop(page) }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

private fun renderPdfFirstPage(file: File): Bitmap {
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        val renderer = PdfRenderer(pfd)
        try {
            renderer.openPage(0).use { page ->
                val height = (RENDER_WIDTH.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(RENDER_WIDTH, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        } finally {
            renderer.close()
        }
    } finally {
        pfd.close()
    }
}

private fun crop(page: Bitmap): Bitmap {
    val w = page.width
    val h = page.height
    val row = IntArray(w)
    val ink = BooleanArray(h)
    for (y in 0 until h step 2) {
        page.getPixels(row, 0, w, 0, y, w, 1)
        var x = 0
        while (x < w) {
            val p = row[x]
            if (Color.red(p) < 200 || Color.green(p) < 200 || Color.blue(p) < 200) {
                ink[y] = true
                break
            }
            x += 2
        }
    }
    val range = ThumbnailCrop.range(ink, w)
    val cropped = Bitmap.createBitmap(page, 0, range.first, w, range.last - range.first + 1)
    val scaledHeight = (cropped.height * THUMB_WIDTH.toFloat() / w).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(cropped, THUMB_WIDTH, scaledHeight, true)
}
