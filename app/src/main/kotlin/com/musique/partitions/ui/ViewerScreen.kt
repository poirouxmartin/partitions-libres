package com.musique.partitions.ui

import android.graphics.Bitmap
import android.graphics.Picture
import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.musique.partitions.data.ScorePaper
import com.musique.partitions.data.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.PartitionsApp
import com.musique.partitions.R
import com.musique.partitions.catalog.Piece
import com.musique.partitions.catalog.ScoreFormat
import com.musique.render.MusicXmlArchive
import com.musique.render.SvgPages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val PageMargin = 12.dp

private sealed interface ViewerState {
    data object Loading : ViewerState
    data class Error(@StringRes val message: Int, val arg: String = "") : ViewerState
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
    val settings by vm.settings.collectAsStateWithLifecycle()
    LaunchedEffect(piece?.id) { piece?.let(vm::ensureThumbnail) }
    val fullScreen = settings.fullScreen
    SystemBarsHidden(fullScreen)
    val setFullScreen = { on: Boolean -> vm.updateSettings { it.copy(fullScreen = on) } }

    Scaffold(
        containerColor = if (settings.paper.isDark) Color(0xFF111317) else MaterialTheme.colorScheme.background,
        topBar = {
            if (!fullScreen) {
                AppTopBar(
                    title = piece?.title ?: stringResource(R.string.viewer_title_fallback),
                    subtitle = piece?.composer,
                    onBack = vm::pop,
                    actions = {
                        IconButton(onClick = { setFullScreen(true) }) {
                            Icon(painterResource(R.drawable.ic_fullscreen), contentDescription = stringResource(R.string.viewer_full_screen))
                        }
                    },
                )
            }
        },
        bottomBar = { if (piece != null && !fullScreen) AttributionStrip(piece) },
        // Full screen: the bars are hidden, only a camera cutout still has to be avoided.
        contentWindowInsets = if (fullScreen) WindowInsets.displayCutout else ScaffoldDefaults.contentWindowInsets,
    ) { pad ->
        BoxWithConstraints(Modifier.padding(pad).fillMaxSize()) {
            val widthPx = with(LocalDensity.current) { (maxWidth - PageMargin * 2).roundToPx() }
            when {
                piece != null -> ViewerBody(
                    piece, widthPx, vm.pageTurns, settings,
                    // The licence still has to be credited: in full screen it closes the last page.
                    footer = if (fullScreen) attributionText(piece) else null,
                    onExitFullScreen = if (fullScreen) ({ setFullScreen(false) }) else null,
                )
                catalog is CatalogState.Loading -> CenteredLoading()
                else -> CenteredMessage(stringResource(R.string.common_piece_not_found))
            }
        }
    }
}

/** Hides the status and navigation bars while [hidden]; a swipe from the edge shows them briefly. */
@Composable
private fun SystemBarsHidden(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, hidden) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) controller?.hide(WindowInsetsCompat.Type.systemBars()) else controller?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun attributionText(piece: Piece): String =
    stringResource(R.string.viewer_attribution, piece.shortLicence(), piece.engraver, piece.source)

@Composable
private fun AttributionStrip(piece: Piece) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            attributionText(piece),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ViewerBody(
    piece: Piece,
    widthPx: Int,
    turns: Flow<Int>,
    settings: Settings,
    footer: String?,
    onExitFullScreen: (() -> Unit)?,
) {
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
        is ViewerState.Error -> CenteredMessage(stringResource(s.message, s.arg))
        is ViewerState.Pictures -> StageReader(s.pages.size, turns, settings, footer, onExitFullScreen) {
            itemsIndexed(s.pages) { _, picture -> PicturePage(picture, settings.paper) }
        }
        is ViewerState.Pdf -> StageReader(s.pages.sizes.size, turns, settings, footer, onExitFullScreen) {
            itemsIndexed(s.pages.sizes) { i, (w, h) -> PdfPage(s.pages, i, w.toFloat() / h, settings.paper) }
        }
    }
}

/**
 * "on the music stand" reading: screen always on, tap on the right to advance one screen,
 * on the left to go back, Bluetooth pedal (arrow keys, next/previous page, space).
 */
@Composable
private fun StageReader(
    pageCount: Int,
    turns: Flow<Int>,
    settings: Settings,
    footer: String?,
    onExitFullScreen: (() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    val tapToTurn = settings.tapToTurn
    val listState = rememberLazyListState()
    val hScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var viewportPx by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(true) }
    val view = LocalView.current
    // Pinch zoom: the pages are laid out [zoom] times wider (sharp vector or re-rasterised PDF),
    // scrolled sideways when wider than the screen. While the fingers move, only a cheap scale
    // ([pinch], around [pinchFocus]) is applied; the layout follows when they lift.
    var zoom by remember { mutableFloatStateOf(1f) }
    var pinch by remember { mutableFloatStateOf(1f) }
    var pinchFocus by remember { mutableStateOf(Offset.Zero) }

    fun commitZoom(factor: Float, focus: Offset) {
        val next = ViewerZoom.clamp(zoom * factor)
        val f = next / zoom
        pinch = 1f
        if (f == 1f) return
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        val x = hScroll.value.toFloat()
        zoom = next
        scope.launch {
            // Two frames: the wider pages are measured, so the new scroll ranges are known.
            withFrameNanos { }
            withFrameNanos { }
            listState.scrollToItem(index, (offset * f).roundToInt())
            listState.scrollBy(focus.y * (f - 1))
            hScroll.scrollTo(ViewerZoom.scrollAfter(x, focus.x, f).roundToInt())
        }
    }

    DisposableEffect(view, settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
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

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewportPx = it.height }
            .pointerInput(Unit) {
                // Initial pass: two fingers are taken before the list sees them, so a pinch never scrolls.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var pinching = false
                    var factor = 1f
                    var focus = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed == 0) break
                        if (pressed >= 2) {
                            if (!pinching) hint = false
                            pinching = true
                            factor *= event.calculateZoom()
                            focus = event.calculateCentroid(useCurrent = true)
                            pinch = (zoom * factor).coerceIn(ViewerZoom.MIN, ViewerZoom.MAX) / zoom
                            pinchFocus = focus
                        }
                        // Once pinching, the remaining finger must not turn into a scroll or a tap.
                        if (pinching) event.changes.forEach { it.consume() }
                    }
                    if (pinching) commitZoom(pinch, focus)
                }
            }
            .pointerInput(tapToTurn) {
                if (tapToTurn) detectTapGestures { offset -> turn(if (offset.x < size.width / 3f) -1 else 1) }
            }
    ) {
        val viewportWidth = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pinch
                    scaleY = pinch
                    if (size.width > 0f && size.height > 0f) {
                        transformOrigin = TransformOrigin(pinchFocus.x / size.width, pinchFocus.y / size.height)
                    }
                }
                .horizontalScroll(hScroll, enabled = zoom > 1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(viewportWidth * zoom).fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = PageMargin, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
                if (footer != null) {
                    item {
                        Text(
                            footer,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.paper.isDark) Color(0xFFB8B4AA) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (zoom > 1f) {
                OverlayChip(onClick = { commitZoom(1f / zoom, Offset.Zero) }, label = stringResource(R.string.viewer_reset_zoom)) {
                    Text(ViewerZoom.label(zoom), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (pageCount > 0) {
                OverlayChip {
                    Text(
                        "${page.coerceIn(1, pageCount)} / $pageCount",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (onExitFullScreen != null) {
                OverlayChip(onClick = onExitFullScreen, label = stringResource(R.string.viewer_exit_full_screen)) {
                    Icon(
                        painterResource(R.drawable.ic_fullscreen_exit),
                        contentDescription = stringResource(R.string.viewer_exit_full_screen),
                        modifier = Modifier.padding(8.dp).size(24.dp),
                    )
                }
            }
        }
        AnimatedVisibility(visible = hint && tapToTurn, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                TapZone("‹", Modifier.weight(1f))
                TapZone("›", Modifier.weight(2f))
            }
        }
    }
}

/** Small translucent pill laid over the score (page number, zoom, exit full screen). */
@Composable
private fun OverlayChip(onClick: (() -> Unit)? = null, label: String? = null, content: @Composable () -> Unit) {
    val color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.75f)
    val contentColor = MaterialTheme.colorScheme.inverseOnSurface
    val shape = RoundedCornerShape(50)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, contentColor = contentColor,
            modifier = Modifier.semantics { if (label != null) contentDescription = label }) { content() }
    } else {
        Surface(shape = shape, color = color, contentColor = contentColor) { content() }
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
        return ViewerState.Error(R.string.viewer_error_offline)
    } catch (e: Exception) {
        return ViewerState.Error(R.string.viewer_error_open, e.message.toString())
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
        ViewerState.Error(R.string.viewer_error_render, e.message.toString())
    }
}

@Composable
private fun PageFrame(aspect: Float, paper: ScorePaper, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().aspectRatio(aspect).shadow(2.dp).background(Color(paper.paper))) { content() }
}

@Composable
private fun PicturePage(picture: Picture, paper: ScorePaper) {
    val aspect = if (picture.width > 0 && picture.height > 0) picture.width.toFloat() / picture.height else 0.7f
    val matrix = remember(paper) { paper.colorMatrix() }
    val layerPaint = remember(paper) {
        matrix?.let { android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(it) } }
    }
    PageFrame(aspect, paper) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                val s = size.width / picture.width.coerceAtLeast(1)
                native.scale(s, s)
                // The SVG is transparent: it is laid over white in a layer recoloured by the matrix.
                if (layerPaint != null) {
                    native.saveLayer(null, layerPaint)
                    native.drawRect(
                        0f, 0f, picture.width.toFloat(), picture.height.toFloat(),
                        android.graphics.Paint().apply { color = android.graphics.Color.WHITE },
                    )
                    native.drawPicture(picture)
                    native.restore()
                } else {
                    native.drawPicture(picture)
                }
                native.restore()
            }
        }
    }
}

@Composable
private fun PdfPage(pdf: PdfPages, index: Int, aspect: Float, paper: ScorePaper) {
    val matrix = remember(paper) { paper.colorMatrix() }
    // Rasterised at the displayed width, so a zoomed page stays sharp; the previous bitmap is
    // kept on screen while the new one renders.
    var widthPx by remember { mutableIntStateOf(0) }
    var bitmap by remember(pdf, index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdf, index, widthPx) {
        if (widthPx > 0) pdf.render(index, ViewerZoom.pdfRenderWidth(widthPx))?.let { bitmap = it }
    }
    PageFrame(aspect, paper, Modifier.onSizeChanged { widthPx = it.width }) {
        bitmap?.let {
            Image(
                it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = matrix?.let { m -> ColorFilter.colorMatrix(ColorMatrix(m)) },
            )
        }
    }
}
