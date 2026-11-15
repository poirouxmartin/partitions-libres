package com.musique.partitions.update

import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.catalog.ReleaseParser
import com.musique.partitions.catalog.isUpdateAvailable
import com.musique.partitions.data.Fetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface UpdatePrefs {
    var lastCheck: Long
    var cachedResponse: String?
    var dismissedTag: String?
}

class UpdateChecker(
    private val fetcher: Fetcher,
    private val prefs: UpdatePrefs,
    private val clock: () -> Long,
    private val currentVersion: String,
    repo: String,
) {
    private val url = "https://api.github.com/repos/$repo/releases?per_page=5"

    /** Dernière version plus récente que la version installée, non masquée ; null sinon (et sur toute erreur). */
    suspend fun check(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val now = clock()
            var response = prefs.cachedResponse
            val age = now - prefs.lastCheck
            if (response == null || age !in 0 until INTERVAL_MS) {
                response = fetcher.open(url).use { it.readBytes().decodeToString() }
                prefs.cachedResponse = response
                prefs.lastCheck = now
            }
            ReleaseParser.latest(response)
                ?.takeIf { isUpdateAvailable(currentVersion, it) && it.tag != prefs.dismissedTag }
        } catch (e: Exception) {
            null
        }
    }

    fun dismiss(tag: String) {
        prefs.dismissedTag = tag
    }

    companion object {
        const val INTERVAL_MS = 12L * 60 * 60 * 1000
    }
}
