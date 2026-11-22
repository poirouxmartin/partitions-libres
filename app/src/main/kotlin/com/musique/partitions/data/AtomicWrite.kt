package com.musique.partitions.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Writes to a temporary sibling file, then renames it: [target] is never half-written. */
fun writeAtomically(target: File, write: (OutputStream) -> Unit) {
    target.absoluteFile.parentFile?.mkdirs()
    val tmp = File(target.absoluteFile.parentFile, target.name + ".tmp")
    try {
        tmp.outputStream().use(write)
        // rename(2) replaces the target atomically on Android at every API level; Files.move
        // (API 26) is only reached off-Android, e.g. JVM tests on Windows.
        if (!tmp.renameTo(target)) @Suppress("NewApi") Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } finally {
        tmp.delete()
    }
}

/**
 * Downloads [url] byte for byte into [target] (atomically).
 * [onProgress] receives 0..1, or -1 when the total size is unknown.
 */
fun download(fetcher: Fetcher, url: String, target: File, onProgress: (Float) -> Unit = {}) {
    fetcher.open(url).use { input ->
        val total = (input as? SizedInputStream)?.length ?: -1L
        writeAtomically(target) { out -> copyWithProgress(input, out, total, onProgress) }
    }
}

private fun copyWithProgress(input: InputStream, out: OutputStream, total: Long, onProgress: (Float) -> Unit) {
    val buffer = ByteArray(32 * 1024)
    var done = 0L
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
        done += n
        onProgress(if (total > 0) (done.toFloat() / total).coerceAtMost(1f) else -1f)
    }
}
