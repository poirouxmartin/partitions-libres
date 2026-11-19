@file:OptIn(ExperimentalLayoutApi::class)

package com.musique.partitions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.R
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ScoreFormat

@Composable
fun PieceScreen(vm: MainViewModel, id: String) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val error by vm.downloadError.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    // States read here (not only inside Scaffold's lambda, a separate recompose scope) so the
    // lookup is redone once the catalogue has loaded.
    val piece = remember(catalog, saved, id) { vm.pieceById(id) }
    val favourite = id in library.favourites

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = "", onBack = vm::pop) {
                if (piece != null) {
                    IconButton(onClick = { vm.toggleFavourite(id) }) {
                        Icon(
                            if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (favourite) stringResource(R.string.piece_remove_favorite) else stringResource(R.string.piece_add_favorite),
                            tint = if (favourite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            when {
                piece != null -> PieceContent(
                    piece = piece,
                    isSaved = saved.any { it.id == id },
                    progress = downloads[id],
                    error = error,
                    onRead = { vm.push(Screen.Viewer(id)) },
                    onSave = { vm.save(piece) },
                    onDelete = { vm.delete(piece) },
                    onComposer = { vm.push(Screen.Composer(piece.composerSort)) },
                )
                catalog is CatalogState.Loading -> CenteredLoading()
                else -> CenteredMessage(stringResource(R.string.common_piece_not_found))
            }
        }
    }
}

@Composable
private fun PieceContent(
    piece: Piece,
    isSaved: Boolean,
    progress: Float?,
    @StringRes error: Int?,
    onRead: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onComposer: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieceBadge(piece, size = 72.dp)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(piece.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (piece.work.isNotEmpty() && piece.work != piece.title) {
                    Text(
                        piece.work,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp).clickable(onClick = onComposer), verticalAlignment = Alignment.CenterVertically) {
            ComposerAvatar(piece.composer, piece.composerSort, size = 36.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(piece.composer, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (piece.lifeDates.isNotEmpty()) {
                    Text(piece.lifeDates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(
            Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (piece.instruments.isNotEmpty()) InfoChip(piece.pictogram(), piece.instruments)
            InfoChip(if (piece.format == ScoreFormat.MUSICXML) "📱" else "📄", stringResource(piece.formatLineRes()))
            if (piece.year.isNotEmpty()) InfoChip("📅", piece.year)
            if (piece.lyricist.isNotEmpty()) InfoChip("✍️", piece.lyricist)
        }

        Button(
            onClick = onRead,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.common_read), style = MaterialTheme.typography.titleMedium)
        }
        when {
            isSaved -> OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_offline), Modifier.weight(1f))
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.piece_delete_offline))
            }
            progress != null -> Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                if (progress >= 0f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (progress >= 0f) {
                    Text(stringResource(R.string.piece_download_percent, (progress * 100).toInt()), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> OutlinedButton(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("⬇", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.piece_keep_offline))
            }
        }
        if (error != null) {
            Text(stringResource(error), modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        AttributionCard(piece, Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

@Composable
private fun InfoChip(icon: String, text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 15.sp)
            Text(
                text,
                Modifier.padding(start = 6.dp).widthIn(max = 220.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AttributionCard(piece: Piece, modifier: Modifier = Modifier) {
    PaperCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🆓", fontSize = 18.sp)
            Text(
                " ${stringResource(R.string.piece_free_title)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        LinkText(piece.licenceText(), piece.licenceUrl, Modifier.padding(top = 8.dp))
        Text(stringResource(R.string.piece_engraver, piece.engraver), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        Column(Modifier.padding(top = 6.dp)) {
            Text(stringResource(R.string.piece_source_label), style = MaterialTheme.typography.bodyMedium)
            LinkText(piece.source, piece.sourceUrl)
        }
        if (piece.edition.isNotEmpty()) {
            Text(stringResource(R.string.piece_original_edition, piece.edition), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        }
        if (piece.isShareAlike) {
            Text(
                stringResource(R.string.piece_sharealike_note),
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
