package com.musique.partitions.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.musique.partitions.R
import com.musique.partitions.catalog.Piece

object Licences {
    private val cc = Regex("^CC-BY(-SA)?-([0-9.]+)$")

    @StringRes
    fun labelRes(code: String): Int? = when (code) {
        "PD" -> R.string.licence_public_domain
        "CC0-1.0" -> R.string.licence_cc0
        else -> null
    }

    fun ccLabel(code: String): String? {
        val m = cc.matchEntire(code) ?: return null
        return "CC BY${if (m.groupValues[1].isNotEmpty()) "-SA" else ""} ${m.groupValues[2]}"
    }
}

@Composable
fun licenceLabel(code: String, fallback: String): String =
    Licences.labelRes(code)?.let { stringResource(it) } ?: Licences.ccLabel(code) ?: fallback

@Composable
fun Piece.licenceText(): String = licenceLabel(licence, licenceLabel)

@Composable
fun Piece.shortLicence(): String = if (licence == "CC0-1.0") "CC0" else licenceText()

@Composable
fun sourceLicenceText(raw: String): String {
    val tokens = raw.split(" / ").map { token ->
        when (token) {
            "PD" -> stringResource(R.string.licence_public_domain)
            "CC0-1.0" -> stringResource(R.string.licence_cc0)
            else -> token
        }
    }
    val joined = tokens.joinToString(" / ")
    return if (tokens.size > 1) stringResource(R.string.about_licence_per_piece, joined) else joined
}
