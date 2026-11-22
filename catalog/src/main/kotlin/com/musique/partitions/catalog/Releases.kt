package com.musique.partitions.catalog

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = (parts.getOrNull(i) ?: 0).compareTo(other.parts.getOrNull(i) ?: 0)
            if (c != 0) return c
        }
        return 0
    }

    companion object {
        fun parse(s: String): AppVersion? {
            val core = s.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            if (core.isEmpty()) return null
            val parts = core.split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}

data class ReleaseInfo(
    val tag: String,
    val version: AppVersion,
    val apkUrl: String,
    val pageUrl: String,
    val notes: String,
)

object ReleaseParser {
    /** Marker in the asset name of the APK built for old devices (legacy flavor). */
    const val LEGACY_MARKER = "legacy"

    /** [legacy] selects the old-device APK of each release instead of the standard one. */
    fun latest(json: String, legacy: Boolean = false): ReleaseInfo? = try {
        val root = Json.parseToJsonElement(json)
        val items = if (root is JsonArray) root.toList() else listOf(root)
        items.mapNotNull { toInfo(it, legacy) }.maxByOrNull { it.version }
    } catch (e: Exception) {
        null
    }

    private fun str(o: JsonObject, key: String): String? = (o[key] as? JsonPrimitive)?.contentOrNull

    private fun toInfo(e: JsonElement, legacy: Boolean): ReleaseInfo? {
        val o = e as? JsonObject ?: return null
        if ((o["draft"] as? JsonPrimitive)?.booleanOrNull == true) return null
        if ((o["prerelease"] as? JsonPrimitive)?.booleanOrNull == true) return null
        val tag = str(o, "tag_name") ?: return null
        val version = AppVersion.parse(tag) ?: return null
        val apk = (o["assets"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { a ->
                val name = str(a, "name") ?: return@firstOrNull false
                name.endsWith(".apk", ignoreCase = true) && name.contains(LEGACY_MARKER, ignoreCase = true) == legacy
            }
            ?.let { str(it, "browser_download_url") }
            ?: return null
        return ReleaseInfo(tag, version, apk, str(o, "html_url") ?: "", str(o, "body") ?: "")
    }
}

fun isUpdateAvailable(current: String, latest: ReleaseInfo?): Boolean {
    val cur = AppVersion.parse(current) ?: return false
    return latest != null && latest.version > cur
}
