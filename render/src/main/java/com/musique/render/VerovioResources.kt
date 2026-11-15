package com.musique.render

import android.content.Context
import android.content.res.AssetManager
import java.io.File

/** Unpacks the bundled Verovio data (assets/verovio-data) to internal storage, once per Verovio tag + app version. */
object VerovioResources {
    private const val VEROVIO_TAG = "version-6.4.1"
    private const val DIR = "verovio-data"

    @Synchronized
    fun ensureUnpacked(context: Context): File {
        val dest = File(context.filesDir, DIR)
        val marker = File(dest, ".unpacked")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val stamp = "$VEROVIO_TAG:${info.longVersionCode}"
        if (marker.isFile && marker.readText() == stamp) return dest
        dest.deleteRecursively()
        copy(context.assets, DIR, dest)
        marker.writeText(stamp)
        return dest
    }

    private fun copy(am: AssetManager, path: String, target: File) {
        val children = am.list(path).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            am.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
        } else {
            target.mkdirs()
            children.forEach { copy(am, "$path/$it", File(target, it)) }
        }
    }
}
