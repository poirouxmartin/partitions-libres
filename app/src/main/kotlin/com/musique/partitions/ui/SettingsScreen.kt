package com.musique.partitions.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.BuildConfig
import com.musique.partitions.data.ScorePaper
import com.musique.partitions.data.ThemeMode

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppNavBar(vm, Screen.Settings) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                "Réglages",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SectionTitle("Partitions")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in ScorePaper.entries) {
                    PaperSwatch(p, p == settings.paper, Modifier.weight(1f)) {
                        vm.updateSettings { it.copy(paper = p) }
                    }
                }
            }

            SectionTitle("Affichage")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in ThemeMode.entries) {
                    ChoiceCard(m == settings.theme, Modifier.weight(1f), onClick = { vm.updateSettings { it.copy(theme = m) } }) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(m.emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
                            Text(m.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            SectionTitle("Lecture")
            SwitchRow("💡", "Écran toujours allumé", settings.keepScreenOn) { v ->
                vm.updateSettings { it.copy(keepScreenOn = v) }
            }
            SwitchRow("👆", "Tourner la page d'un toucher", settings.tapToTurn) { v ->
                vm.updateSettings { it.copy(tapToTurn = v) }
            }

            SectionTitle("Appli")
            Row(
                Modifier.fillMaxWidth().clickable { vm.push(Screen.About) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("À propos et licences", Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceCard(selected: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) { content() }
}

@Composable
private fun PaperSwatch(paper: ScorePaper, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    ChoiceCard(selected, modifier, onClick) {
        Column(Modifier.padding(6.dp)) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(0.75f)) {
                drawRect(Color(paper.paper))
                val ink = Color(paper.ink)
                drawStaff(ink, size.height * 0.22f)
                drawStaff(ink, size.height * 0.60f)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(paper.label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Une portée miniature : 5 lignes, quelques notes avec leur hampe. */
private fun DrawScope.drawStaff(ink: Color, top: Float) {
    val gap = size.height * 0.06f
    val left = size.width * 0.10f
    val right = size.width * 0.90f
    val stroke = 1.dp.toPx()
    for (i in 0 until 5) {
        val y = top + i * gap
        drawLine(ink, Offset(left, y), Offset(right, y), strokeWidth = stroke)
    }
    val headW = gap * 1.3f
    val headH = gap * 0.9f
    val notes = 4
    for (n in 0 until notes) {
        val x = left + (right - left) * (0.12f + 0.24f * n)
        // Hauteurs variées, en demi-intervalles de portée.
        val y = top + gap * listOf(3f, 2f, 2.5f, 1.5f)[n]
        drawOval(ink, Offset(x, y - headH / 2), Size(headW, headH))
        drawLine(ink, Offset(x + headW, y), Offset(x + headW, y - gap * 2.5f), strokeWidth = stroke)
    }
}

@Composable
private fun SwitchRow(emoji: String, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(label, Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
