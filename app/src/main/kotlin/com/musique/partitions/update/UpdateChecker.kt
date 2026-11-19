package com.musique.partitions.update

import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.catalog.ReleaseParser
import com.musique.partitions.catalog.isUpdateAvailable
import com.musique.partitions.data.Fetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UpdatePrefs {
    var lastCheck: Long
    var lastFailure: Long
    var cachedResponse: String?
    var dismissedTag: String?
    var dismissedAt: Long
}

/**
 * Queries the GitHub releases of [repo] (public repository: the app has no token).
 * Called at startup and on each return to the foreground; the network is only hit
 * once every [INTERVAL_MS] when all is well, and [RETRY_MS] after a failure.
 * Offline, an update already known stays offered.
 */
class UpdateChecker(
    private val fetcher: Fetcher,
    private val prefs: UpdatePrefs,
    private val clock: () -> Long,
    private val currentVersion: String,
    repo: String,
) {
    private val url = "https://api.github.com/repos/$repo/releases?per_page=5"

    /** Only APKs published in this repository's releases are accepted. */
    val apkUrlPrefix = "https://github.com/$repo/releases/download/"

    /** Latest version newer than the installed one, not postponed; null otherwise (never throws). */
    suspend fun check(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val now = clock()
        var response = prefs.cachedResponse
        if (isDue(now, cached = response != null)) {
            try {
                response = fetcher.open(url).use { it.readBytes().decodeToString() }
                prefs.cachedResponse = response
                prefs.lastCheck = now
            } catch (e: Exception) {
                prefs.lastFailure = now
            }
        }
        response
            ?.let(ReleaseParser::latest)
            ?.takeIf { isUpdateAvailable(currentVersion, it) && it.apkUrl.startsWith(apkUrlPrefix) && !isSnoozed(it, now) }
    }

    /** « Plus tard » (later): the banner comes back after [SNOOZE_MS], or right away for a newer version. */
    fun dismiss(tag: String) {
        prefs.dismissedTag = tag
        prefs.dismissedAt = clock()
    }

    private fun isDue(now: Long, cached: Boolean): Boolean {
        if (prefs.lastFailure > 0 && now - prefs.lastFailure in 0 until RETRY_MS) return false
        return !cached || now - prefs.lastCheck !in 0 until INTERVAL_MS
    }

    private fun isSnoozed(info: ReleaseInfo, now: Long): Boolean =
        info.tag == prefs.dismissedTag && now - prefs.dismissedAt in 0 until SNOOZE_MS

    companion object {
        const val INTERVAL_MS = 6L * 60 * 60 * 1000
        const val RETRY_MS = 30L * 60 * 1000
        const val SNOOZE_MS = 24L * 60 * 60 * 1000
    }
}
