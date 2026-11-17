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
 * Interroge les releases GitHub de [repo] (dépôt public : l'appli n'a pas de jeton).
 * Appelé au démarrage et à chaque retour au premier plan ; le réseau n'est sollicité
 * qu'une fois toutes les [INTERVAL_MS] quand tout va bien, [RETRY_MS] après un échec.
 * Hors ligne, une mise à jour déjà connue reste proposée.
 */
class UpdateChecker(
    private val fetcher: Fetcher,
    private val prefs: UpdatePrefs,
    private val clock: () -> Long,
    private val currentVersion: String,
    repo: String,
) {
    private val url = "https://api.github.com/repos/$repo/releases?per_page=5"

    /** Seuls les APK publiés dans les releases de ce dépôt sont acceptés. */
    val apkUrlPrefix = "https://github.com/$repo/releases/download/"

    /** Dernière version plus récente que la version installée, non reportée ; null sinon (jamais d'exception). */
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

    /** « Plus tard » : la bannière revient après [SNOOZE_MS], ou tout de suite pour une version plus récente. */
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
