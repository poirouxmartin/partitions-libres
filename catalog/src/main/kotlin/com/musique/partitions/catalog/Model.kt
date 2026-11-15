package com.musique.partitions.catalog

import kotlinx.serialization.Serializable

enum class ScoreFormat { MUSICXML, PDF }

@Serializable
data class Catalog(
    val schema: Int,
    val generated: String,
    val policy: String = "",
    val sources: List<CatalogSource> = emptyList(),
    val pieces: List<Piece>,
)

@Serializable
data class CatalogSource(val name: String, val url: String, val licence: String)

@Serializable
data class Piece(
    val id: String,
    val title: String,
    val work: String = "",
    val composer: String,
    val composerSort: String,
    val born: Int? = null,
    val died: Int? = null,
    val instruments: String = "",
    val lyricist: String = "",
    val year: String = "",
    val licence: String,
    val licenceLabel: String,
    val licenceUrl: String,
    val engraver: String,
    val source: String,
    val sourceUrl: String = "",
    val edition: String = "",
    val musicxmlUrl: String? = null,
    val pdfUrl: String? = null,
) {
    val format: ScoreFormat get() = if (musicxmlUrl != null) ScoreFormat.MUSICXML else ScoreFormat.PDF
    val fileUrl: String get() = musicxmlUrl ?: pdfUrl!!
    val fileExtension: String get() = if (format == ScoreFormat.MUSICXML) "mxl" else "pdf"
    val lifeDates: String
        get() = when {
            born == null -> ""
            died == null -> "$born–"
            else -> "$born–$died"
        }
    val isShareAlike: Boolean get() = licence.startsWith("CC-BY-SA")
}
