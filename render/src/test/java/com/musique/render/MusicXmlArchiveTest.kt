package com.musique.render

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MusicXmlArchiveTest {
    private val xml = "<score-partwise><part-name>é</part-name></score-partwise>"

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val container = """<container><rootfiles><rootfile full-path="score.xml"/></rootfiles></container>"""

    @Test fun mxlWithContainer() {
        val bytes = zip(
            "other.xml" to "<x/>".toByteArray(),
            "META-INF/container.xml" to container.toByteArray(),
            "score.xml" to xml.toByteArray(),
        )
        assertEquals(xml, MusicXmlArchive.readMusicXml(bytes))
    }

    @Test fun mxlWithoutContainer() {
        val bytes = zip("META-INF/foo.xml" to "<x/>".toByteArray(), "song.musicxml" to xml.toByteArray())
        assertEquals(xml, MusicXmlArchive.readMusicXml(bytes))
    }

    @Test fun mxlWithBom() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = zip("META-INF/container.xml" to container.toByteArray(), "score.xml" to bom + xml.toByteArray())
        assertEquals(xml, MusicXmlArchive.readMusicXml(bytes))
    }

    @Test fun plainXml() {
        assertEquals(xml, MusicXmlArchive.readMusicXml(xml.toByteArray()))
    }

    @Test fun utf16Bom() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + xml.toByteArray(Charsets.UTF_16LE)
        assertEquals(xml, MusicXmlArchive.readMusicXml(bytes))
    }

    @Test fun garbageFails() {
        assertFailsWith<IllegalArgumentException> { MusicXmlArchive.readMusicXml(ByteArray(0)) }
        assertFailsWith<IllegalArgumentException> { MusicXmlArchive.readMusicXml(zip("readme.txt" to "hi".toByteArray())) }
        assertFailsWith<IllegalArgumentException> { MusicXmlArchive.readMusicXml("PK garbage".toByteArray()) }
    }
}
