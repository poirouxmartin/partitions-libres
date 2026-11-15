package com.musique.partitions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.catalog.Piece
import androidx.compose.runtime.remember

@Composable
fun PieceScreen(vm: MainViewModel, id: String) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val error by vm.downloadError.collectAsStateWithLifecycle()
    // States read here (not only inside Scaffold's lambda, a separate recompose scope) so the
    // lookup is redone once the catalogue has loaded.
    val piece = remember(catalog, saved, id) { vm.pieceById(id) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Partition", onBack = vm::pop) },
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
                )
                catalog is CatalogState.Loading -> CenteredLoading()
                else -> CenteredMessage("Partition introuvable.")
            }
        }
    }
}

@Composable
private fun PieceContent(
    piece: Piece,
    isSaved: Boolean,
    progress: Float?,
    error: String?,
    onRead: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(piece.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            listOf(piece.composer, piece.lifeDates).filter { it.isNotEmpty() }.joinToString("  "),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        listOf(
            piece.work to null,
            piece.instruments to null,
            piece.year to "Année",
            piece.lyricist to "Paroles",
        ).forEach { (value, label) ->
            if (value.isNotEmpty()) {
                Text(
                    if (label == null) value else "$label : $value",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Text(
            piece.formatLine(),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onRead, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text("Lire la partition")
        }
        when {
            isSaved -> OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Supprimer du téléphone")
            }
            progress != null -> Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (progress >= 0f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    if (progress >= 0f) "Téléchargement… ${(progress * 100).toInt()} %" else "Téléchargement…",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            else -> OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Télécharger")
            }
        }
        if (error != null) {
            Text(error, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        AttributionCard(piece, Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

@Composable
fun AttributionCard(piece: Piece, modifier: Modifier = Modifier) {
    PaperCard(modifier.fillMaxWidth()) {
        Text("Attribution", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        LinkText(piece.licenceLabel, piece.licenceUrl, Modifier.padding(top = 8.dp))
        Text("Gravure : ${piece.engraver}", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        Column(Modifier.padding(top = 6.dp)) {
            Text("Source :", style = MaterialTheme.typography.bodyMedium)
            LinkText(piece.source, piece.sourceUrl)
        }
        if (piece.edition.isNotEmpty()) {
            Text("Édition d'origine : ${piece.edition}", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        }
        if (piece.isShareAlike) {
            Text(
                "Fichier diffusé tel quel, sans modification.",
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
