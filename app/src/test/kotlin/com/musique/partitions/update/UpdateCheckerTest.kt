package com.musique.partitions.update

import com.musique.partitions.data.Fetcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UpdateCheckerTest {

    private class FakePrefs : UpdatePrefs {
        override var lastCheck = 0L
        override var cachedResponse: String? = null
        override var dismissedTag: String? = null
        override var lastFailure = 0L
        override var dismissedAt = 0L
    }

    private fun releases(vararg tags: String) = tags.joinToString(prefix = "[", postfix = "]") {
        """{"tag_name":"$it","html_url":"https://example.org/$it","body":"",
           "assets":[{"name":"app.apk","browser_download_url":"https://github.com/owner/repo/releases/download/$it/app.apk"}]}"""
    }

    private var body = releases("v0.2.0")
    private var fail = false
    private var fetches = 0
    private var now = 1_000_000L
    private val prefs = FakePrefs()

    private val fetcher = Fetcher {
        fetches++
        if (fail) throw IOException("hors ligne")
        ByteArrayInputStream(body.toByteArray())
    }

    private fun checker(current: String = "0.1.0") =
        UpdateChecker(fetcher, prefs, { now }, current, "owner/repo")

    @Test
    fun newerReleaseIsAvailable() = runTest {
        val info = assertNotNull(checker().check())
        assertEquals("v0.2.0", info.tag)
        assertEquals("https://github.com/owner/repo/releases/download/v0.2.0/app.apk", info.apkUrl)
    }

    @Test
    fun sameOrOlderReleaseIsNotAvailable() = runTest {
        assertNull(checker("0.2.0").check())
        prefs.lastCheck = 0; prefs.cachedResponse = null
        assertNull(checker("0.3.0").check())
    }

    @Test
    fun throttledToOncePerInterval() = runTest {
        val c = checker()
        assertNotNull(c.check())
        now += 60 * 60 * 1000
        assertNotNull(c.check()) // résultat servi depuis le cache
        assertEquals(1, fetches)
        now += UpdateChecker.INTERVAL_MS
        c.check()
        assertEquals(2, fetches)
    }

    @Test
    fun networkErrorMeansNoUpdate() = runTest {
        fail = true
        assertNull(checker().check())
        assertEquals(0L, prefs.lastCheck)
        assertEquals(now, prefs.lastFailure)
    }

    @Test
    fun dismissedVersionIsHiddenUntilNewerOne() = runTest {
        val c = checker()
        c.dismiss("v0.2.0")
        assertNull(c.check())
        body = releases("v0.2.0", "v0.3.0")
        now += UpdateChecker.INTERVAL_MS
        assertEquals("v0.3.0", c.check()?.tag)
    }

    @Test
    fun knownUpdateStaysOfferedOffline() = runTest {
        val c = checker()
        assertNotNull(c.check())
        fail = true
        now += UpdateChecker.INTERVAL_MS
        assertEquals("v0.2.0", c.check()?.tag)
        assertEquals(2, fetches)
    }

    @Test
    fun retriesThirtyMinutesAfterFailure() = runTest {
        fail = true
        val c = checker()
        c.check()
        assertEquals(1, fetches)
        now += 10 * 60 * 1000
        c.check()
        assertEquals(1, fetches)
        fail = false
        now += UpdateChecker.RETRY_MS
        assertNotNull(c.check())
        assertEquals(2, fetches)
    }

    @Test
    fun laterSnoozesForADay() = runTest {
        val c = checker()
        c.dismiss("v0.2.0")
        assertNull(c.check())
        now += UpdateChecker.SNOOZE_MS
        assertEquals("v0.2.0", c.check()?.tag)
    }

    @Test
    fun apkFromAnotherHostIsIgnored() = runTest {
        body = """[{"tag_name":"v0.2.0","assets":[{"name":"app.apk","browser_download_url":"https://evil.example/v0.2.0/app.apk"}]}]"""
        assertNull(checker().check())
    }
}
