package com.musique.partitions.data

import android.content.Context
import com.musique.partitions.catalog.Catalog
import com.musique.partitions.catalog.CatalogParser
import com.musique.partitions.catalog.CatalogSearch
import com.musique.partitions.catalog.ComposerEntry
import com.musique.partitions.catalog.InstrumentFamily
import com.musique.partitions.catalog.Piece
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadedCatalog(val catalog: Catalog) {
    val search = CatalogSearch(catalog.pieces)
    val composers: List<ComposerEntry> = search.composers()
    val byId: Map<String, Piece> = catalog.pieces.associateBy { it.id }
    val families: Map<String, Set<InstrumentFamily>> = catalog.pieces.associate { it.id to InstrumentFamily.of(it.instruments) }
    val familyCounts: Map<InstrumentFamily, Int> = InstrumentFamily.entries.associateWith { f -> families.values.count { f in it } }
}

class CatalogRepository(private val context: Context) {
    @Volatile
    private var loaded: LoadedCatalog? = null

    suspend fun get(): LoadedCatalog = loaded ?: withContext(Dispatchers.IO) {
        val text = context.assets.open("catalog.json").use { it.readBytes().decodeToString() }
        LoadedCatalog(CatalogParser.parse(text))
    }.also { loaded = it }
}
