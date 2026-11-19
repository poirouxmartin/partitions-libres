package com.musique.partitions.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.musique.partitions.PartitionsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musique.partitions.catalog.InstrumentFamily
import com.musique.partitions.catalog.Piece

/** Colour pictogram of each family: readable at a glance, no text. */
val InstrumentFamily.emoji: String
    get() = when (this) {
        InstrumentFamily.VOICE -> "🎤"
        InstrumentFamily.PIANO -> "🎹"
        InstrumentFamily.ORGAN -> "⛪"
        InstrumentFamily.GUITAR -> "🎸"
        InstrumentFamily.STRINGS -> "🎻"
        InstrumentFamily.WINDS -> "🎺"
    }

/** Tints matching the paper and ink theme; one per composer, stable from one launch to the next. */
private val Inks = listOf(
    Color(0xFF23395B), Color(0xFFA0552A), Color(0xFF5C7A5A), Color(0xFF7A4E7E),
    Color(0xFF2F6F7A), Color(0xFF8A6A2F), Color(0xFF9B3D4A), Color(0xFF4F5D8A),
)

fun inkFor(key: String): Color = Inks[Math.floorMod(key.hashCode(), Inks.size)]

fun Piece.pictogram(): String = InstrumentFamily.of(instruments).firstOrNull()?.emoji ?: "🎼"

/** Initial of the family name (sorting starts with the name). */
fun initialOf(sort: String, name: String): String =
    sort.ifBlank { name }.firstOrNull { it.isLetter() }?.uppercase() ?: "♪"

@Composable
fun ComposerAvatar(name: String, sort: String, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(inkFor(sort.ifBlank { name })),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialOf(sort, name),
            style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp),
            color = Color.White,
        )
    }
}

/** Badge of a piece: instrument pictogram on the composer's tint; [family] (the active filter) wins when the piece has it. */
@Composable
fun PieceBadge(piece: Piece, size: Dp = 48.dp, family: InstrumentFamily? = null) {
    val families = InstrumentFamily.of(piece.instruments)
    val shown = family?.takeIf { it in families } ?: families.firstOrNull()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(inkFor(piece.composerSort).copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(shown?.emoji ?: "🎼", fontSize = (size.value * 0.5f).sp)
    }
}

private val thumbCache = LruCache<String, ImageBitmap>(40)

/** Decoded preview of [id] (memory cache, then disk), or null while loading or when absent. */
@Composable
fun rememberThumbnail(id: String, hasThumb: Boolean): ImageBitmap? {
    if (!hasThumb) return null
    val app = LocalContext.current.applicationContext as PartitionsApp
    val bitmap by produceState(thumbCache.get(id), id) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { app.thumbnailStore.load(id)?.asImageBitmap() }?.also { thumbCache.put(id, it) }
        }
    }
    return bitmap
}

/** Preview of the first system when available, otherwise the pictogram badge. */
@Composable
fun PieceThumb(piece: Piece, size: Dp, family: InstrumentFamily? = null, hasThumb: Boolean) {
    val bitmap = rememberThumbnail(piece.id, hasThumb)
    if (bitmap == null) {
        PieceBadge(piece, size, family)
        return
    }
    val shape = RoundedCornerShape(size * 0.18f)
    Image(
        bitmap,
        contentDescription = null,
        modifier = Modifier.size(size).clip(shape).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outline, shape),
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopStart,
    )
}
