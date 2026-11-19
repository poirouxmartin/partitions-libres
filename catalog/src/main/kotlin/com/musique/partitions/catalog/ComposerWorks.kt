package com.musique.partitions.catalog

/** Pieces of one instrument family (null: other instruments). */
data class WorkGroup(val family: InstrumentFamily?, val pieces: List<Piece>)

object ComposerWorks {
    /** Exact match on [composerSort] (no fuzzy search); biggest groups first, "other" last. */
    fun of(pieces: List<Piece>, composerSort: String): List<WorkGroup> =
        pieces.filter { it.composerSort == composerSort }
            .groupBy { p -> InstrumentFamily.of(p.instruments).firstOrNull() }
            .map { (family, ps) -> WorkGroup(family, ps.sortedWith { a, b -> NaturalOrder.compare(a.title, b.title) }) }
            .sortedWith(
                compareBy<WorkGroup> { it.family == null }
                    .thenByDescending { it.pieces.size }
                    .thenBy { it.family?.ordinal ?: 0 },
            )
}
