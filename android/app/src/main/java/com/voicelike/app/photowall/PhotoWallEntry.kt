package com.voicelike.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelike.app.keep.loadKeepGalleryThumbBitmap
import com.voicelike.app.keep.peekBestKeepGalleryThumbBitmap

// Height and per-column layout of the corner of wall on the home entry (iOS PhotoWallPreviewMosaic):
// three staggered columns, tilted, fading into the card on the left. The first photos the wall opens with.
internal val PHOTO_WALL_ENTRY_HEIGHT = 116.dp
private val MOSAIC_COLUMNS =
    listOf(
        -18 to listOf(62, 48, 70),
        -44 to listOf(54, 74, 50),
        -8 to listOf(70, 52, 60),
    )
private const val MOSAIC_THUMB_BUCKET = 224

@Composable
internal fun PhotoWallPreviewMosaic(
    items: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    val placeholder = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
    Box(
        modifier =
            modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    // Fades toward the title, which sits on the leading side.
                    val fade =
                        if (layoutDirection == LayoutDirection.Rtl) {
                            Brush.horizontalGradient(0.68f to Color.Black, 1f to Color.Transparent)
                        } else {
                            Brush.horizontalGradient(0f to Color.Transparent, 0.32f to Color.Black)
                        }
                    drawRect(brush = fade, blendMode = BlendMode.DstIn)
                },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .wrapContentSize(unbounded = true)
                    .offset(x = 6.dp, y = 4.dp)
                    .graphicsLayer { rotationZ = -9f },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MOSAIC_COLUMNS.forEachIndexed { column, (offset, heights) ->
                Column(
                    modifier = Modifier.offset(y = offset.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    heights.forEachIndexed { row, height ->
                        MosaicTile(
                            item = items.getOrNull(column * 3 + row),
                            height = height,
                            placeholder = placeholder,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MosaicTile(
    item: MediaItem?,
    height: Int,
    placeholder: Color,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(9.dp)
    var bitmap by remember(item?.id) {
        mutableStateOf(item?.let { peekBestKeepGalleryThumbBitmap(it, MOSAIC_THUMB_BUCKET) })
    }
    LaunchedEffect(item?.id) {
        if (item != null && bitmap == null) {
            bitmap = loadKeepGalleryThumbBitmap(context, item, MOSAIC_THUMB_BUCKET)
        }
    }
    Box(
        modifier =
            Modifier
                .size(width = 50.dp, height = height.dp)
                .clip(shape)
                .background(placeholder),
    ) {
        bitmap?.let { loaded ->
            val image = remember(loaded) { loaded.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// Solid brand capsule; every language says NEW.
@Composable
internal fun PhotoWallNewBadge() {
    Box(
        modifier =
            Modifier
                .height(18.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "NEW",
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
