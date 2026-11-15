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
    fun latest(json: String): ReleaseInfo? = try {
        val root = Json.parseToJsonElement(json)
        val items = if (root is JsonArray) root.toList() else listOf(root)
        items.mapNotNull(::toInfo).maxByOrNull { it.version }
    } catch (e: Exception) {
        null
    }

    private fun str(o: JsonObject, key: String): String? = (o[key] as? JsonPrimitive)?.contentOrNull

    private fun toInfo(e: JsonElement): ReleaseInfo? {
        val o = e as? JsonObject ?: return null
        if ((o["draft"] as? JsonPrimitive)?.booleanOrNull == true) return null
        if ((o["prerelease"] as? JsonPrimitive)?.booleanOrNull == true) return null
        val tag = str(o, "tag_name") ?: return null
        val version = AppVersion.parse(tag) ?: return null
        val apk = (o["assets"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { str(it, "name")?.endsWith(".apk", ignoreCase = true) == true }
            ?.let { str(it, "browser_download_url") }
            ?: return null
        return ReleaseInfo(tag, version, apk, str(o, "html_url") ?: "", str(o, "body") ?: "")
    }
}

fun isUpdateAvailable(current: String, latest: ReleaseInfo?): Boolean {
    val cur = AppVersion.parse(current) ?: return false
    return latest != null && latest.version > cur
}
