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
import androidx.compose.ui.res.pluralStringResource
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
        topBar = { AppTopBar(title = stringResource(R.string.about_title), onBack = vm::pop) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle(stringResource(R.string.about_section_catalog))
            if (ready != null) {
                val catalog = ready.data.catalog
                Text(stringResource(R.string.about_policy),style = MaterialTheme.typography.bodyLarge)
                Text(
                    pluralStringResource(R.plurals.about_catalog_summary, catalog.pieces.size, catalog.pieces.size, catalog.generated.take(10)),
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )

                SectionTitle(stringResource(R.string.about_section_sources))
                catalog.sources.forEach { s ->
                    PaperCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall)
                        LinkText(s.url, s.url)
                        Text(sourceLicenceText(s.licence), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                SectionTitle(stringResource(R.string.about_section_licenses))
                val counts = remember(catalog) {
                    catalog.pieces.groupBy { it.licence }.map { (code, ps) -> Triple(code, ps.first().licenceLabel, ps.size) }
                        .sortedByDescending { it.third }
                }
                counts.forEach { (code, fallback, n) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(licenceLabel(code, fallback), style = MaterialTheme.typography.bodyMedium)
                        Text(n.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                stringResource(R.string.about_sources_notice),
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle(stringResource(R.string.about_section_third_party))
            listOf(
                Triple("Verovio", "LGPL-3.0", "https://www.verovio.org"),
                Triple("androidsvg", "Apache-2.0", "https://github.com/BigBadaboom/androidsvg"),
                Triple("Liberation Serif", "SIL OFL 1.1", "https://github.com/liberationfonts/liberation-fonts"),
                Triple(stringResource(R.string.about_androidx), "Apache-2.0", "https://developer.android.com/jetpack"),
            ).forEach { (name, licence, url) ->
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text("$name ($licence)", style = MaterialTheme.typography.bodyLarge)
                    LinkText(url, url)
                }
            }
            Text(
                stringResource(R.string.about_verovio_notice),
                Modifier.padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
