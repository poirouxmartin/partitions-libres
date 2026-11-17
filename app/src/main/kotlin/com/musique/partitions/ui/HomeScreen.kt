package com.musique.partitions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.R
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ReleaseInfo

@Composable
fun HomeScreen(vm: MainViewModel) {
    val query by vm.query.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val installing by vm.installing.collectAsStateWithLifecycle()
    val updateMessage by vm.updateMessage.collectAsStateWithLifecycle()
    val savedIds = remember(saved) { saved.map { it.id }.toSet() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = { vm.push(Screen.About) }) {
                    Icon(Icons.Default.Info, contentDescription = "À propos et licences")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Compositeur, titre, œuvre…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            update?.let { info ->
                UpdateBanner(
                    info = info,
                    installing = installing,
                    message = updateMessage,
                    onInstall = { vm.installUpdate(info) },
                    onLater = { vm.dismissUpdate(info) },
                )
            }
            when (val c = catalog) {
                CatalogState.Loading -> CenteredLoading()
                is CatalogState.Failed -> CenteredMessage("Le catalogue n'a pas pu être chargé (${c.message}).")
                is CatalogState.Ready -> {
                    if (query.isBlank()) {
                        BrowseList(vm, c, saved, savedIds)
                    } else {
                        ResultList(vm, results, savedIds)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(info: ReleaseInfo, installing: Boolean, message: String?, onInstall: () -> Unit, onLater: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                "Version ${info.tag.removePrefix("v")} disponible",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onLater) { Text("Plus tard") }
                Button(onClick = onInstall, enabled = !installing) {
                    Text(if (installing) "Téléchargement…" else "Installer")
                }
            }
        }
    }
}

@Composable
private fun BrowseList(vm: MainViewModel, c: CatalogState.Ready, saved: List<Piece>, savedIds: Set<String>) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        if (saved.isNotEmpty()) {
            item { SectionTitle("Mes partitions") }
            items(saved, key = { "s-" + it.id }) { p ->
                PieceRow(p, saved = true) { vm.push(Screen.Piece(p.id)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item { SectionTitle("Compositeurs") }
        items(c.data.composers, key = { "c-" + it.sort }) { comp ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.setQuery(comp.name) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(comp.name, style = MaterialTheme.typography.bodyLarge)
                    if (comp.lifeDates.isNotEmpty()) {
                        Text(comp.lifeDates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(comp.count.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ResultList(vm: MainViewModel, results: List<Piece>, savedIds: Set<String>) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Text(
                when {
                    results.isEmpty() -> "Aucun résultat"
                    results.size == 1 -> "1 résultat"
                    results.size >= 300 -> "300 premiers résultats"
                    else -> "${results.size} résultats"
                },
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(results, key = { it.id }) { p ->
            PieceRow(p, saved = p.id in savedIds) { vm.push(Screen.Piece(p.id)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun PieceRow(piece: Piece, saved: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(
            piece.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(piece.composer, piece.lifeDates).filter { it.isNotEmpty() }.joinToString("  "),
            style = MaterialTheme.typography.bodyMedium,
        )
        val detail = listOf(piece.work, piece.instruments).filter { it.isNotEmpty() }.joinToString(" · ")
        if (detail.isNotEmpty()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            LicenceChip(piece.shortLicence())
            Text(
                piece.source,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (saved) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Disponible hors ligne",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
