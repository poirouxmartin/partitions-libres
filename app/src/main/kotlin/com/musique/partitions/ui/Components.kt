@file:OptIn(ExperimentalMaterial3Api::class)

package com.musique.partitions.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musique.partitions.R
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ScoreFormat

fun Piece.shortLicence(): String = if (licence == "CC0-1.0") "CC0" else licenceLabel

@StringRes
fun Piece.formatLineRes(): Int = when (format) {
    ScoreFormat.MUSICXML -> R.string.piece_format_adapted
    ScoreFormat.PDF -> R.string.piece_format_pdf
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // No browser: nothing to do.
    }
}

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun AppNavBar(vm: MainViewModel, current: Screen) {
    val library by vm.library.collectAsStateWithLifecycle()
    // Couleurs de la palette « papier et encre » plutôt que le mauve par défaut de Material.
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        NavigationBarItem(
            selected = current == Screen.Home,
            onClick = { vm.openTab(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Accueil") },
        )
        NavigationBarItem(
            selected = current == Screen.Favourites,
            onClick = { vm.openTab(Screen.Favourites) },
            icon = {
                BadgedBox(badge = { if (library.favourites.isNotEmpty()) Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text(library.favourites.size.toString()) } }) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                }
            },
            label = { Text("Favoris") },
        )
        NavigationBarItem(
            selected = current == Screen.Settings,
            onClick = { vm.openTab(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Réglages") },
        )
    }
}

@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun LicenceChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
fun LinkText(text: String, url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        text,
        modifier = modifier.clickable(enabled = url.isNotBlank()) { openUrl(context, url) },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = if (url.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            textDecoration = if (url.isBlank()) TextDecoration.None else TextDecoration.Underline,
        ),
    )
}

@Composable
fun CenteredLoading(text: String? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (text != null) Text(text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
