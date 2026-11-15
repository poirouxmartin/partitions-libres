package com.musique.partitions.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleasesTest {
    private fun v(s: String) = AppVersion.parse(s)!!

    @Test
    fun versionOrder() {
        assertTrue(v("0.1.0") < v("0.1.1"))
        assertTrue(v("0.1.1") < v("0.2"))
        assertTrue(v("0.2") < v("1.0.0"))
        assertEquals(0, v("1.0").compareTo(v("1.0.0")))
        assertEquals(listOf(1, 2, 0), v("v1.2.0").parts)
        assertEquals(listOf(0, 1, 0), v("v0.1.0-beta").parts)
        assertNull(AppVersion.parse("abc"))
    }

    private fun rel(tag: String, draft: Boolean = false, pre: Boolean = false, asset: String? = "app.apk") = """
        {"tag_name":"$tag","draft":$draft,"prerelease":$pre,"html_url":"http://page/$tag","body":null,
         "assets":[${asset?.let { """{"name":"$it","browser_download_url":"http://dl/$tag/$it"}""" } ?: ""}]}
    """.trimIndent()

    @Test
    fun latestSkipsInvalid() {
        val json = "[" + listOf(
            rel("v0.3.0", pre = true),
            rel("v0.2.5", draft = true),
            rel("v0.2.0", asset = "notes.txt"),
            rel("v0.1.0"),
            rel("v0.1.5"),
        ).joinToString(",") + "]"
        val r = assertNotNull(ReleaseParser.latest(json))
        assertEquals("v0.1.5", r.tag)
        assertEquals("http://dl/v0.1.5/app.apk", r.apkUrl)
        assertEquals("http://page/v0.1.5", r.pageUrl)
        assertEquals("", r.notes)
    }

    @Test
    fun singleObjectAndMalformed() {
        assertEquals("v1.0.0", ReleaseParser.latest(rel("v1.0.0"))?.tag)
        assertNull(ReleaseParser.latest("{not json"))
        assertNull(ReleaseParser.latest("[]"))
    }

    @Test
    fun updateAvailable() {
        val r = ReleaseParser.latest(rel("v0.2.0"))
        assertTrue(isUpdateAvailable("0.1.0", r))
        assertFalse(isUpdateAvailable("0.2.0", r))
        assertFalse(isUpdateAvailable("0.3", r))
        assertFalse(isUpdateAvailable("0.1.0", null))
    }
}
