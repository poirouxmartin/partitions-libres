package com.musique.partitions.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** PDF pages rendered on demand; PdfRenderer only accepts one open page at a time, hence the Mutex. */
class PdfPages private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val sizes: List<Pair<Int, Int>>,
) {
    private val mutex = Mutex()
    @Volatile
    private var closed = false
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? {
        val key = "$index@$widthPx"
        cache.get(key)?.let { return it }
        return mutex.withLock {
            cache.get(key) ?: if (closed) null else withContext(Dispatchers.Default) {
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bitmap)
                    bitmap
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun close() {
        closed = true
        GlobalScope.launch(Dispatchers.IO) {
            mutex.withLock {
                renderer.close()
                pfd.close()
            }
        }
    }

    companion object {
        fun open(file: File): PdfPages {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer = PdfRenderer(pfd)
                val sizes = (0 until renderer.pageCount).map { i -> renderer.openPage(i).use { it.width to it.height } }
                return PdfPages(pfd, renderer, sizes)
            } catch (e: Exception) {
                pfd.close()
                throw e
            }
        }
    }
}
