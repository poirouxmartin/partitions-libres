package com.musique.partitions.data

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

fun interface Fetcher {
    fun open(url: String): InputStream
}

/** Stream whose total size is known (for progress); [length] is -1 otherwise. */
class SizedInputStream(
    input: InputStream,
    val length: Long,
    private val onClose: () -> Unit = {},
) : FilterInputStream(input) {
    override fun close() {
        try {
            super.close()
        } finally {
            onClose()
        }
    }
}

class HttpFetcher(private val userAgent: String) : Fetcher {

    override fun open(url: String): InputStream {
        var current = URL(url)
        repeat(MAX_REDIRECTS) {
            val c = current.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", userAgent)
            c.setRequestProperty("Accept-Encoding", "identity")
            val code = c.responseCode
            if (code in 300..399 && code != 304) {
                val location = c.getHeaderField("Location")
                c.disconnect()
                if (location == null) throw IOException("Redirection sans destination")
                // URL(base, loc) also handles http -> https and relative paths.
                current = URL(current, location)
                return@repeat
            }
            if (code !in 200..299) {
                c.disconnect()
                throw IOException("HTTP $code")
            }
            return SizedInputStream(c.inputStream, c.contentLengthLong) { c.disconnect() }
        }
        throw IOException("Trop de redirections")
    }

    private companion object {
        const val MAX_REDIRECTS = 8
    }
}
