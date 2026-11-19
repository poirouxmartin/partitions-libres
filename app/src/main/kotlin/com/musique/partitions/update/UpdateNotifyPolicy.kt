package com.musique.partitions.update

import com.musique.partitions.catalog.AppVersion

/** Pure rules for the background "update available" notification (unit-tested). */
object UpdateNotifyPolicy {
    /** Notify a release only if it is newer than the installed version and than the last notified one. */
    fun shouldNotify(current: String, latestTag: String, lastNotifiedTag: String?): Boolean {
        val latest = AppVersion.parse(latestTag) ?: return false
        val cur = AppVersion.parse(current) ?: return false
        if (latest <= cur) return false
        val notified = lastNotifiedTag?.let(AppVersion::parse) ?: return true
        return latest > notified
    }

    /** A posted notification is stale once the installed version caught up with it. */
    fun isStale(current: String, lastNotifiedTag: String?): Boolean {
        val notified = lastNotifiedTag?.let(AppVersion::parse) ?: return false
        val cur = AppVersion.parse(current) ?: return false
        return cur >= notified
    }
}
