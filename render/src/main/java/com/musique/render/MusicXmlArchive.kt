package com.musique.render

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Extracts MusicXML text from a compressed .mxl archive or a plain XML file. Pure JVM. */
object MusicXmlArchive {
    fun readMusicXml(bytes: ByteArray): String {
        val xml = if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            fromZip(bytes)
        } else {
            bytes
        }
        val text = decode(xml)
        require(text.isNotBlank()) { "Empty MusicXML content" }
        return text
    }

    private fun fromZip(bytes: ByteArray): ByteArray {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (!e.isDirectory) entries[e.name] = zip.readBytes()
            }
        }
        val container = entries["META-INF/container.xml"]
        if (container != null) {
            val m = Regex("""<rootfile\b[^>]*?\bfull-path\s*=\s*["']([^"']+)["']""").find(decode(container))
            val path = m?.groupValues?.get(1)
            if (path != null) entries[path]?.let { return it }
        }
        val fallback = entries.entries.firstOrNull { (name, _) ->
            !name.startsWith("META-INF/") && (name.endsWith(".xml", true) || name.endsWith(".musicxml", true))
        }
        return fallback?.value ?: throw IllegalArgumentException("No MusicXML file found in .mxl archive")
    }

    private fun decode(b: ByteArray): String = when {
        b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte() -> String(b, 2, b.size - 2, Charsets.UTF_16BE)
        b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte() -> String(b, 2, b.size - 2, Charsets.UTF_16LE)
        b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte() ->
            String(b, 3, b.size - 3, Charsets.UTF_8)
        else -> String(b, Charsets.UTF_8)
    }
}
