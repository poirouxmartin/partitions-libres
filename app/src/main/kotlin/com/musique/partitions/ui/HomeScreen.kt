package com.musique.partitions.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.R
import com.musique.partitions.catalog.InstrumentFamily
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ReleaseInfo
import com.musique.partitions.data.Library
import java.time.LocalDate

@Composable
fun HomeScreen(vm: MainViewModel) {
    val query by vm.query.collectAsStateWithLifecycle()
    val family by vm.family.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val installing by vm.installing.collectAsStateWithLifecycle()
    val updateMessage by vm.updateMessage.collectAsStateWithLifecycle()
    val savedIds = remember(saved) { saved.map { it.id }.toSet() }
    val favIds = remember(library) { library.favourites.toSet() }

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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Compositeur, titre…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            val ready = catalog as? CatalogState.Ready
            FamilyChips(selected = family, counts = ready?.data?.familyCounts.orEmpty(), onSelect = vm::setFamily)
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
                    if (query.isBlank() && family == null) {
                        BrowseList(vm, c, library, saved, savedIds, favIds)
                    } else {
                        ResultList(vm, results, savedIds, favIds, onReset = { vm.setQuery(""); vm.setFamily(null) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyChips(selected: InstrumentFamily?, counts: Map<InstrumentFamily, Int>, onSelect: (InstrumentFamily?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(InstrumentFamily.entries.filter { (counts[it] ?: 0) > 0 || counts.isEmpty() }, key = { it.name }) { f ->
            FilterChip(
                selected = f == selected,
                onClick = { onSelect(if (f == selected) null else f) },
                label = { Text(f.label) },
                leadingIcon = { Text(f.emoji, fontSize = 18.sp) },
                shape = MaterialTheme.shapes.large,
            )
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

/** Pièce du jour : la même pour tout le monde un jour donné, pour avoir toujours quelque chose à ouvrir. */
private fun pieceOfTheDay(c: CatalogState.Ready): Piece? {
    val pieces = c.data.catalog.pieces
    if (pieces.isEmpty()) return null
    return pieces[Math.floorMod(LocalDate.now().toEpochDay() * 7919L, pieces.size.toLong()).toInt()]
}

@Composable
private fun BrowseList(
    vm: MainViewModel,
    c: CatalogState.Ready,
    library: Library,
    saved: List<Piece>,
    savedIds: Set<String>,
    favIds: Set<String>,
) {
    val recents = remember(library, c) { library.recents.mapNotNull { vm.pieceById(it) } }
    val favourites = remember(library, c, saved) { library.favourites.mapNotNull { vm.pieceById(it) } }
    val daily = remember(c) { pieceOfTheDay(c) }
    val open = { p: Piece -> vm.push(Screen.Piece(p.id)) }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        val hero = recents.firstOrNull()
        val heroPiece = hero ?: daily
        if (heroPiece != null) {
            item(key = "hero") {
                HeroCard(
                    piece = heroPiece,
                    resume = hero != null,
                    onRead = { vm.push(Screen.Viewer(heroPiece.id)) },
                    onOpen = { open(heroPiece) },
                )
            }
        }
        if (favourites.isNotEmpty()) {
            item(key = "fav") { Shelf(Icons.Default.Favorite, "Favoris", favourites, savedIds, favIds, open) }
        }
        if (recents.size > 1) {
            item(key = "rec") { Shelf(Icons.Default.Refresh, "Récents", recents.drop(1), savedIds, favIds, open) }
        }
        if (saved.isNotEmpty()) {
            item(key = "off") { Shelf(Icons.Default.CheckCircle, "Hors ligne", saved, savedIds, favIds, open) }
        }
        item(key = "comp-title") { SectionTitle("Compositeurs", Modifier.padding(horizontal = 16.dp)) }
        items(c.data.composers, key = { "c-" + it.sort }) { comp ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.setQuery(comp.name) }.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposerAvatar(comp.name, comp.sort)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(comp.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (comp.lifeDates.isNotEmpty()) {
                        Text(comp.lifeDates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CountPill(comp.count)
            }
        }
    }
}

@Composable
private fun CountPill(n: Int) {
    Text(
        n.toString(),
        modifier = Modifier
            .padding(start = 8.dp)
            .then(Modifier),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun HeroCard(piece: Piece, resume: Boolean, onRead: () -> Unit, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PieceBadge(piece, size = 64.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (resume) Icons.Default.Refresh else Icons.Default.Star,
                        contentDescription = if (resume) "Reprendre" else "Partition du jour",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (resume) " Reprendre" else " Du jour",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    piece.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    piece.composer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Button(
            onClick = onRead,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lire", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Shelf(
    icon: ImageVector,
    title: String,
    pieces: List<Piece>,
    savedIds: Set<String>,
    favIds: Set<String>,
    onOpen: (Piece) -> Unit,
) {
    Column {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(
                "  $title  ${pieces.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pieces, key = { it.id }) { p -> PieceTile(p, p.id in savedIds, p.id in favIds) { onOpen(p) } }
        }
    }
}

@Composable
private fun PieceTile(piece: Piece, saved: Boolean, favourite: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                PieceBadge(piece, size = 44.dp)
                Spacer(Modifier.weight(1f))
                StatusIcons(saved, favourite)
            }
            Text(
                piece.title,
                modifier = Modifier.padding(top = 8.dp).height(40.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                piece.composer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusIcons(saved: Boolean, favourite: Boolean) {
    Row {
        if (favourite) {
            Icon(Icons.Default.Favorite, contentDescription = "Favori", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
        if (saved) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Hors ligne", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ResultList(vm: MainViewModel, results: List<Piece>, savedIds: Set<String>, favIds: Set<String>, onReset: () -> Unit) {
    if (results.isEmpty()) {
        // Pas d'impasse : une action pour repartir.
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎼", fontSize = 48.sp)
            Text("Aucun résultat", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onReset, modifier = Modifier.padding(top = 16.dp)) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Tout effacer")
            }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        item {
            Text(
                "♪ " + if (results.size == 300) "300+" else results.size.toString(),
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(results, key = { it.id }) { p ->
            PieceRow(p, saved = p.id in savedIds, favourite = p.id in favIds) { vm.push(Screen.Piece(p.id)) }
        }
    }
}

@Composable
private fun PieceRow(piece: Piece, saved: Boolean, favourite: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PieceBadge(piece)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                piece.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    piece.composer,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LicenceChip(piece.shortLicence())
            }
        }
        StatusIcons(saved, favourite)
    }
}
