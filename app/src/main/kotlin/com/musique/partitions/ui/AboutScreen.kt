package com.musique.partitions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.BuildConfig
import com.musique.partitions.R

@Composable
fun AboutScreen(vm: MainViewModel) {
    val catalogState by vm.catalog.collectAsStateWithLifecycle()
    val ready = catalogState as? CatalogState.Ready

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "À propos et licences", onBack = vm::pop) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Catalogue")
            if (ready != null) {
                val catalog = ready.data.catalog
                Text(catalog.policy, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${catalog.pieces.size} partitions, catalogue généré le ${catalog.generated.take(10)}.",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )

                SectionTitle("Sources")
                catalog.sources.forEach { s ->
                    PaperCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall)
                        LinkText(s.url, s.url)
                        Text(s.licence, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionTitle("Partitions par licence")
                val counts = remember(catalog) {
                    catalog.pieces.groupingBy { it.licenceLabel }.eachCount().entries.sortedByDescending { it.value }
                }
                counts.forEach { (label, n) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(n.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "Les fichiers proviennent d'OpenScore (CC0) et de Mutopia (licence propre à chaque partition). " +
                    "Ils sont téléchargés et conservés tels quels, sans modification.",
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle("Composants tiers")
            listOf(
                Triple("Verovio", "LGPL-3.0", "https://www.verovio.org"),
                Triple("androidsvg", "Apache-2.0", "https://github.com/BigBadaboom/androidsvg"),
                Triple("AndroidX et Jetpack Compose", "Apache-2.0", "https://developer.android.com/jetpack"),
            ).forEach { (name, licence, url) ->
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text("$name ($licence)", style = MaterialTheme.typography.bodyLarge)
                    LinkText(url, url)
                }
            }
            Text(
                "Verovio est lié dynamiquement ; ses sources sont disponibles sur le site du projet.",
                Modifier.padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
