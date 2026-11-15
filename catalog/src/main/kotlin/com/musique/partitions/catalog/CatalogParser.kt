package com.musique.partitions.catalog

import kotlinx.serialization.json.Json

object CatalogParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val allowedPrefixes = listOf("PD", "CC0", "CC-BY-")

    fun parse(json: String): Catalog {
        val c = this.json.decodeFromString<Catalog>(json)
        return c.copy(pieces = c.pieces.filter(::isAcceptable))
    }

    private fun isAcceptable(p: Piece): Boolean {
        if (p.musicxmlUrl == null && p.pdfUrl == null) return false
        if (allowedPrefixes.none { p.licence.startsWith(it) }) return false
        val parts = p.licence.split('-')
        return parts.none { it == "NC" || it == "ND" }
    }
}
