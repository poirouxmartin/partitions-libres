package com.musique.partitions.catalog

data class ComposerEntry(val name: String, val sort: String, val lifeDates: String, val count: Int)

class CatalogSearch(private val pieces: List<Piece>) {

    private class Keys(val piece: Piece, val composer: List<String>, val title: List<String>, val instruments: List<String>)

    private val index: List<Keys> = pieces.map { p ->
        fun keys(s: String) = TextNormalizer.tokens(s).map(TextNormalizer::phoneticKey).distinct()
        Keys(p, keys(p.composer + " " + p.composerSort), keys(p.title + " " + p.work), keys(p.instruments))
    }

    fun search(query: String, limit: Int = 300): List<Piece> {
        val q = TextNormalizer.tokens(query).map(TextNormalizer::phoneticKey)
        if (q.isEmpty()) return emptyList()
        val scored = ArrayList<Pair<Int, Piece>>()
        for (k in index) {
            var total = 0
            var ok = true
            for (qk in q) {
                val s = maxOf(fieldScore(qk, k.composer, 3), fieldScore(qk, k.title, 2), fieldScore(qk, k.instruments, 1))
                if (s == 0) { ok = false; break }
                total += s
            }
            if (ok) scored += total to k.piece
        }
        return scored
            .sortedWith(
                compareByDescending<Pair<Int, Piece>> { it.first }
                    .thenBy { it.second.composerSort }
                    .thenComparator { a, b -> NaturalOrder.compare(a.second.title, b.second.title) },
            )
            .take(limit)
            .map { it.second }
    }

    fun composers(): List<ComposerEntry> =
        pieces.groupBy { it.composerSort }
            .map { (sort, ps) ->
                val first = ps.first()
                ComposerEntry(first.composer, sort, first.lifeDates, ps.size)
            }
            .sortedBy { TextNormalizer.normalize(it.sort) }

    private fun fieldScore(qk: String, field: List<String>, weight: Int): Int {
        var best = 0
        for (fk in field) {
            val exact = fk == qk
            val match = exact ||
                (qk.length >= 2 && fk.startsWith(qk)) ||
                (qk.length >= 5 && levenshtein(qk, fk, if (qk.length >= 9) 2 else 1) <= (if (qk.length >= 9) 2 else 1))
            if (match) {
                val s = weight + if (exact) 1 else 0
                if (s > best) best = s
            }
        }
        return best
    }

    /** Levenshtein distance, returns max+1 early when it surely exceeds [max]. */
    private fun levenshtein(a: String, b: String, max: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
