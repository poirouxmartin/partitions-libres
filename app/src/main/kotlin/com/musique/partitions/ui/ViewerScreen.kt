package com.musique.partitions.ui

import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.PartitionsApp
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ScoreFormat
import com.musique.render.MusicXmlArchive
import com.musique.render.SvgPages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private val PageMargin = 12.dp

private sealed interface ViewerState {
    data object Loading : ViewerState
    data class Error(val message: String) : ViewerState
    data class Pictures(val pages: List<Picture>) : ViewerState
    data class Pdf(val pages: PdfPages) : ViewerState
}

@Composable
fun ViewerScreen(vm: MainViewModel, id: String) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val saved by vm.savedPieces.collectAsStateWithLifecycle()
    // Read both states here, in this scope: BoxWithConstraints below is its own recompose scope,
    // so a lookup that only depends on states read there would never be refreshed.
    val piece = remember(catalog, saved, id) { vm.pieceById(id) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = piece?.title ?: "Partition", subtitle = piece?.composer, onBack = vm::pop) },
        bottomBar = { if (piece != null) AttributionStrip(piece) },
    ) { pad ->
        BoxWithConstraints(Modifier.padding(pad).fillMaxSize()) {
            val widthPx = with(LocalDensity.current) { (maxWidth - PageMargin * 2).roundToPx() }
            when {
                piece != null -> ViewerBody(piece, widthPx, vm.pageTurns)
                catalog is CatalogState.Loading -> CenteredLoading()
                else -> CenteredMessage("Partition introuvable.")
            }
        }
    }
}

@Composable
private fun AttributionStrip(piece: Piece) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            "${piece.shortLicence()} · Gravure : ${piece.engraver} · Source : ${piece.source}",
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ViewerBody(piece: Piece, widthPx: Int, turns: Flow<Int>) {
    val app = LocalContext.current.applicationContext as PartitionsApp
    // A PDF is redrawn at the column width without reloading; only the MusicXML layout depends on it.
    val layoutWidth = if (piece.format == ScoreFormat.MUSICXML) widthPx else 0
    var state by remember(piece.id, layoutWidth) { mutableStateOf<ViewerState>(ViewerState.Loading) }

    LaunchedEffect(piece.id, layoutWidth) { state = loadViewer(app, piece, layoutWidth) }
    DisposableEffect(state) {
        val s = state
        onDispose { if (s is ViewerState.Pdf) s.pages.close() }
    }

    when (val s = state) {
        ViewerState.Loading -> CenteredLoading()
        is ViewerState.Error -> CenteredMessage(s.message)
        is ViewerState.Pictures -> StageReader(s.pages.size, turns) {
            itemsIndexed(s.pages) { _, picture -> PicturePage(picture) }
        }
        is ViewerState.Pdf -> StageReader(s.pages.sizes.size, turns) {
            itemsIndexed(s.pages.sizes) { i, (w, h) -> PdfPage(s.pages, i, w.toFloat() / h, widthPx) }
        }
    }
}

/**
 * « sur le pupitre » reading: screen always on, tap on the right to advance one screen,
 * on the left to go back, Bluetooth pedal (arrow keys, next/previous page, space).
 */
@Composable
private fun StageReader(pageCount: Int, turns: Flow<Int>, content: LazyListScope.() -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var viewportPx by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(true) }
    val view = LocalView.current

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        delay(1800)
        hint = false
    }
    fun turn(direction: Int) {
        hint = false
        scope.launch { listState.animateScrollBy(direction * viewportPx * 0.85f) }
    }
    LaunchedEffect(turns) { turns.collect { turn(it) } }
    val page by remember {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val index = listState.firstVisibleItemIndex
            if (first != null && listState.firstVisibleItemScrollOffset > first.size / 2) index + 2 else index + 1
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewportPx = it.height }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { offset -> turn(if (offset.x < size.width / 3f) -1 else 1) }
            },
            contentPadding = PaddingValues(horizontal = PageMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        if (pageCount > 0) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.75f),
            ) {
                Text(
                    "${page.coerceIn(1, pageCount)} / $pageCount",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
        AnimatedVisibility(visible = hint, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                TapZone("‹", Modifier.weight(1f))
                TapZone("›", Modifier.weight(2f))
            }
        }
    }
}

@Composable
private fun TapZone(arrow: String, modifier: Modifier) {
    Box(
        modifier.fillMaxSize().padding(4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(arrow, fontSize = 64.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    }
}

private suspend fun loadViewer(app: PartitionsApp, piece: Piece, widthPx: Int): ViewerState {
    val file = try {
        app.scoreStore.fileForViewing(piece)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        return ViewerState.Error("Pas de connexion. Téléchargez la partition pour la lire hors ligne.")
    } catch (e: Exception) {
        return ViewerState.Error("Impossible d'ouvrir la partition (${e.message}).")
    }
    return try {
        when (piece.format) {
            ScoreFormat.MUSICXML -> {
                val xml = withContext(Dispatchers.IO) { MusicXmlArchive.readMusicXml(file.readBytes()) }
                val pages = withContext(app.renderDispatcher) {
                    app.renderer.render(xml, widthPx, 1f).map(SvgPages::toPicture)
                }
                ViewerState.Pictures(pages)
            }
            ScoreFormat.PDF -> ViewerState.Pdf(withContext(Dispatchers.IO) { PdfPages.open(file) })
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ViewerState.Error("Impossible d'afficher cette partition (${e.message}).")
    }
}

@Composable
private fun PageFrame(aspect: Float, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(aspect).shadow(2.dp).background(Color.White)) { content() }
}

@Composable
private fun PicturePage(picture: Picture) {
    val aspect = if (picture.width > 0 && picture.height > 0) picture.width.toFloat() / picture.height else 0.7f
    PageFrame(aspect) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                val s = size.width / picture.width.coerceAtLeast(1)
                native.scale(s, s)
                native.drawPicture(picture)
                native.restore()
            }
        }
    }
}

@Composable
private fun PdfPage(pdf: PdfPages, index: Int, aspect: Float, widthPx: Int) {
    PageFrame(aspect) {
        val bitmap by produceState<Bitmap?>(null, pdf, index, widthPx) { value = pdf.render(index, widthPx) }
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
    }
}
