package com.musique.partitions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.R
import kotlinx.coroutines.launch

@Composable
fun FavouritesScreen(vm: MainViewModel) {
    // States read here, at the root: the Scaffold has its own recomposition lambdas.
    val library by vm.library.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val thumbs by vm.thumbnails.collectAsStateWithLifecycle()
    val pieces = remember(library, catalog, saved) { library.favourites.mapNotNull { vm.pieceById(it) } }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMsg = stringResource(R.string.favourites_removed)
    val undoLabel = stringResource(R.string.favourites_undo)

    fun remove(id: String) {
        vm.toggleFavourite(id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(removedMsg, actionLabel = undoLabel)
            if (result == SnackbarResult.ActionPerformed) vm.toggleFavourite(id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppNavBar(vm, Screen.Favourites) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    "  " + stringResource(R.string.favourites_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "  ${pieces.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            when {
                pieces.isEmpty() && catalog is CatalogState.Loading -> CenteredLoading()
                pieces.isEmpty() -> EmptyFavourites(onBrowse = { vm.openTab(Screen.Home) })
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(pieces, key = { it.id }) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.push(Screen.Piece(p.id)) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PieceThumb(p, size = 56.dp, hasThumb = p.id in thumbs)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    p.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    p.composer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { vm.push(Screen.Viewer(p.id)) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.common_read))
                            }
                            IconButton(onClick = { remove(p.id) }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = stringResource(R.string.piece_remove_favorite),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavourites(onBrowse: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(stringResource(R.string.favourites_empty_title), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.favourites_empty_hint),
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBrowse, modifier = Modifier.padding(top = 20.dp)) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.favourites_browse))
        }
    }
}
