package com.musique.partitions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.R
import com.musique.partitions.catalog.ComposerWorks

@Composable
fun ComposerScreen(vm: MainViewModel, sort: String) {
    val catalogState by vm.catalog.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val ready = catalogState as? CatalogState.Ready
    val composer = remember(ready) { ready?.data?.composers?.firstOrNull { it.sort == sort } }
    val groups = remember(ready, sort) { ready?.let { ComposerWorks.of(it.data.catalog.pieces, sort) }.orEmpty() }
    val savedIds = remember(saved) { saved.map { it.id }.toSet() }
    val favIds = remember(library) { library.favourites.toSet() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = composer?.name.orEmpty(), onBack = vm::pop) },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            when {
                composer != null -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    item(key = "header") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ComposerAvatar(composer.name, composer.sort, size = 72.dp)
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(composer.name, style = MaterialTheme.typography.headlineSmall)
                                if (composer.lifeDates.isNotEmpty()) {
                                    Text(composer.lifeDates, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    pluralStringResource(R.plurals.composer_scores, composer.count, composer.count),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    groups.forEach { g ->
                        item(key = "g-" + (g.family?.name ?: "other")) {
                            val name = g.family?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.composer_group_other)
                            SectionTitle("${g.family?.emoji ?: "🎼"}  $name (${g.pieces.size})")
                        }
                        items(g.pieces, key = { it.id }) { p ->
                            PieceRow(p, p.id in savedIds, p.id in favIds) { vm.push(Screen.Piece(p.id)) }
                        }
                    }
                }
                catalogState is CatalogState.Loading -> CenteredLoading()
                else -> CenteredMessage(stringResource(R.string.composer_not_found))
            }
        }
    }
}
