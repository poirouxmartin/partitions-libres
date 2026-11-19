package com.musique.partitions.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File

/** Cropped first-page previews, one small WebP per piece. */
class ThumbnailStore(private val dir: File) {
    private fun file(id: String) = File(dir, "$id.webp")

    fun has(id: String): Boolean = file(id).exists()

    fun load(id: String): Bitmap? = BitmapFactory.decodeFile(file(id).path)

    @Suppress("DEPRECATION")
    fun save(id: String, bitmap: Bitmap) {
        // WEBP_LOSSY exists from API 30; plain WEBP is the lossy one before.
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        writeAtomically(file(id)) { bitmap.compress(format, 80, it) }
    }

    fun ids(): Set<String> =
        dir.listFiles { f -> f.extension == "webp" }?.map { it.nameWithoutExtension }?.toSet().orEmpty()
}
