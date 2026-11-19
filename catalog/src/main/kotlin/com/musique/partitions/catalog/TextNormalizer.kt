package com.musique.partitions.catalog

import java.text.Normalizer

object TextNormalizer {
    private val combining = Regex("\\p{Mn}+")
    private val nonAlnum = Regex("[^a-z0-9]+")
    private val repeated = Regex("(.)\\1+")
    private val endFv = Regex("[fv]$")

    private fun isPlain(s: String): Boolean = s.all { it in 'a'..'z' || it in '0'..'9' }

    fun normalize(s: String): String {
        val lower = s.lowercase()
            .replace("ø", "o").replace("æ", "ae").replace("œ", "oe").replace("ß", "ss")
        val stripped = combining.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
        return nonAlnum.replace(stripped, " ").trim()
    }

    fun tokens(s: String): List<String> =
        normalize(s).split(' ').filter { it.isNotEmpty() }

    /** Folds transliteration variants (Rachmaninov/Rachmaninoff, Tchaikovsky/Tschaikowsky...). */
    fun phoneticKey(token: String): String {
        var t = if (isPlain(token)) token else normalize(token).replace(" ", "")
        t = t.replace("oe", "o").replace("ue", "u").replace("ae", "a")
        t = t.replace("tsch", "ch").replace("tch", "ch").replace("sch", "s")
        t = t.replace("kh", "h").replace("ch", "h")
        t = t.replace("w", "v").replace("ou", "u")
        t = t.replace("y", "i").replace("j", "i")
        t = t.replace("c", "k").replace("q", "k")
        t = t.replace(repeated, "$1")
        t = t.replace(endFv, "v")
        t = t.replace("ii", "i")
        if (t.length > 5 && t.endsWith("e")) t = t.dropLast(1)
        return t
    }
}
