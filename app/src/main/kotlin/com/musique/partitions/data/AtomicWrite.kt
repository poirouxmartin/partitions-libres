package com.musique.partitions.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Écrit dans un fichier temporaire voisin puis renomme : [target] n'existe jamais à moitié écrit. */
fun writeAtomically(target: File, write: (OutputStream) -> Unit) {
    target.absoluteFile.parentFile?.mkdirs()
    val tmp = File(target.absoluteFile.parentFile, target.name + ".tmp")
    try {
        tmp.outputStream().use(write)
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } finally {
        tmp.delete()
    }
}

/**
 * Télécharge [url] octet pour octet dans [target] (atomique).
 * [onProgress] reçoit 0..1, ou -1 quand la taille totale est inconnue.
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
