@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.voicelike.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Global cache for PhotoCard enriched data to avoid repeated EXIF reads.
 * Thread-safe and memory-efficient.
 */
object PhotoCardDataCache {
    private val cache = ConcurrentHashMap<Long, MediaItem>()
    private const val MAX_CACHE_SIZE = 200 // 限制缓存大小
    
    fun get(id: Long): MediaItem? = cache[id]
    
    fun put(id: Long, item: MediaItem) {
        // 简单的 LRU：如果超过限制，清理一半
        if (cache.size >= MAX_CACHE_SIZE) {
            val toRemove = cache.keys.take(MAX_CACHE_SIZE / 2)
            toRemove.forEach { cache.remove(it) }
        }
        cache[id] = item
    }
    
    fun clear() {
        cache.clear()
    }
}

@Composable
fun PhotoCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    isTopCard: Boolean = true,
    stackIndex: Int = 0,
    isExpanded: Boolean = false,
    muteVideos: Boolean = true,
    onToggleExpand: () -> Unit = {},
    showActions: Boolean = true,
    onDataLoaded: (MediaItem) -> Unit = {}
) {
    var isMuted by remember(item.id, muteVideos) { mutableStateOf(muteVideos) }
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cachedItem = remember(item.id) { PhotoCardDataCache.get(item.id) }
    val chromeButtonBackground = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        Color.Black.copy(alpha = 0.2f)
    }
    val chromeButtonIcon = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White

    // EXIF State with cache
    var exifLoaded by remember(item.id) { mutableStateOf(cachedItem != null) }
    var currentItem by remember(item.id) { mutableStateOf(cachedItem ?: item) }
    val stillImageRequest = rememberStillImageRequest(item.uri, crossfade = false)
    val stillImageNoFadeRequest = rememberStillImageRequest(currentItem.uri, crossfade = false)
    val videoFrameRequest = rememberVideoFrameImageRequest(item.uri, crossfade = false)
    val livePlaybackItem = remember(currentItem.id, currentItem.uri, currentItem.livePhotoVideoUri, currentItem.isMotionPhoto) {
        if (currentItem.isMotionPhoto) {
            currentItem
        } else {
            currentItem.copy(uri = currentItem.livePhotoVideoUri ?: currentItem.uri, mimeType = "video/mp4")
        }
    }

    // Load EXIF with cache
    LaunchedEffect(item.id) {
        // 检查缓存
        val cached = PhotoCardDataCache.get(item.id)
        if (cached != null) {
            currentItem = cached
            exifLoaded = true
            onDataLoaded(cached)
            return@LaunchedEffect
        }
        
        // 只在缓存未命中且是照片时才执行
        if (!exifLoaded && item.type == "photo") {
            withContext(Dispatchers.IO) {
                try {
                    val previewInsights = MediaAnalysisManager.resolveCardPreviewInsights(
                        context = context,
                        item = item,
                        timeBudgetMs = 900L
                    )

                    context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                        val cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: "--"
                        val shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "--"
                        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "--"
                        val focalLength = resolvePreferredCardExifFocalLength(exif)
                        
                        val location = previewInsights.location

                        val camera = if (cameraMake.isNotEmpty() || cameraModel.isNotEmpty()) "$cameraMake $cameraModel".trim() else "--"
                        
                        // Format Aperture (f/1.8) and Focal Length (26mm)
                        val fNumber = if (aperture != "--") "f/$aperture" else "--"
                        val focal = formatCardFocalLength(focalLength)

                        val enrichedItem = item.copy(
                            camera = camera,
                            iso = iso,
                            shutter = formatShutterSpeed(shutter),
                            focalLength = focal,
                            aperture = fNumber,
                            location = location,
                            isMotionPhoto = previewInsights.motionInfo?.isMotionPhoto == true,
                            motionPhotoVideoOffset = previewInsights.motionInfo?.videoOffset ?: 0L,
                            motionPhotoVideoLength = previewInsights.motionInfo?.videoLength ?: 0L
                        )
                        
                        // 存入缓存
                        PhotoCardDataCache.put(item.id, enrichedItem)
                        currentItem = enrichedItem
                        exifLoaded = true
                        onDataLoaded(enrichedItem)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .clip(shape)
    ) {
        // --- Media Content ---
        if (item.type == "video") {
            if (isTopCard && !isExpanded) {
                VideoPlayer(item = item, isMuted = isMuted, shouldPlay = isTopCard)
                
                if (showActions) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .bounceClick(onClick = { isMuted = !isMuted })
                            .background(
                                chromeButtonBackground,
                                CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                    ) {
                        AnimatedContent(
                            targetState = isMuted,
                            transitionSpec = {
                                scaleIn() togetherWith scaleOut()
                            },
                            label = "mute_icon"
                        ) { muted ->
                            Icon(
                                imageVector = if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                contentDescription = "Mute",
                                tint = chromeButtonIcon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    SubcomposeAsyncImage(
                        model = videoFrameRequest,
                        loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = "Video",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                }
            }
        } else if ((currentItem.isLivePhoto && currentItem.livePhotoVideoUri != null) || currentItem.isMotionPhoto) {
            Box(Modifier.fillMaxSize()) {
                SubcomposeAsyncImage(
                    model = stillImageNoFadeRequest,
                    loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (isTopCard && !isExpanded) {
                     VideoPlayer(
                         item = livePlaybackItem,
                         isMuted = isMuted,
                         shouldPlay = isTopCard,
                         playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                     )
                     
                     Box(
                         modifier = Modifier
                             .align(Alignment.TopStart)
                             .padding(16.dp)
                             .background(
                                 MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                 RoundedCornerShape(4.dp)
                             )
                             .padding(horizontal = 6.dp, vertical = 2.dp)
                     ) {
                         Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                     }
                     
                     if (showActions) {
                         IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .bounceClick(onClick = { isMuted = !isMuted })
                                .clip(CircleShape)
                                .background(
                                    chromeButtonBackground
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
                        ) {
                            AnimatedContent(
                                targetState = isMuted,
                                transitionSpec = {
                                    scaleIn() togetherWith scaleOut()
                                },
                                label = "mute_icon_live"
                            ) { muted ->
                                Icon(
                                    imageVector = if (muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                     }
                } else {
                     Box(
                         modifier = Modifier
                             .align(Alignment.TopStart)
                             .padding(16.dp)
                             .background(
                                 MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                 RoundedCornerShape(4.dp)
                             )
                             .padding(horizontal = 6.dp, vertical = 2.dp)
                     ) {
                         Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                     }
                }
            }
        } else {
            // Photo
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(
                    model = stillImageRequest,
                    loading = { SkeletonLoader(Modifier.fillMaxSize()) },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                )
                
                val badgeText = getFormatBadgeText(item)
                if (badgeText != null) {
                    Box(
                         modifier = Modifier
                             .align(Alignment.TopStart)
                             .padding(16.dp)
                             .background(
                                 MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                 RoundedCornerShape(4.dp)
                             )
                             .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                         Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
        
        // Info Button
        if (!isExpanded && showActions) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(32.dp) // Smaller button size
                    .bounceClick(onClick = onToggleExpand)
                    .clip(CircleShape)
                    .background(
                        chromeButtonBackground
                    )
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Info",
                    tint = chromeButtonIcon,
                    modifier = Modifier.size(16.dp) // Smaller icon size
                )
            }
        }
        
        // Expanded EXIF Overlay (Identical to DraggableCard)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb clicks */ }
            ) {
                // Close Button
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp)
                        .bottomNavigationBarPadding(24.dp)
                        .size(32.dp) // Smaller button size
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp), // Smaller icon size
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .offset(y = (-40).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val date = Date(item.dateAdded * 1000)
                    val format = java.text.SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    
                    Text(
                        format.format(date),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Filename
                    Text(
                        currentItem.displayName, 
                        fontFamily = FontFamily.Serif, 
                        fontSize = 16.sp, 
                        textAlign = TextAlign.Center,
                        maxLines = 2, 
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.filename ?: "Filename",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Grid
                    Row(Modifier.fillMaxWidth()) {
                         Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Image,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.resolution.ifEmpty { "--" }, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.resolution ?: "Resolution",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val sizeStr = if (currentItem.size > 1024 * 1024) String.format("%.1f MB", currentItem.size / 1024.0 / 1024.0) else String.format("%.0f KB", currentItem.size / 1024.0)
                                Icon(
                                    Icons.Outlined.Save,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(sizeStr, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.size ?: "Size",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                         Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.PhotoCamera,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.camera, fontFamily = FontFamily.Serif, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(
                                    strings.camera ?: "Camera",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(if (currentItem.location == "Unknown Location") strings.unknownLocation ?: "Unknown" else currentItem.location, fontFamily = FontFamily.Serif, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(
                                    strings.location ?: "Location",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth()) {
                         Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Iso,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.iso, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.iso ?: "ISO",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.shutter, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.shutter ?: "Shutter",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(Modifier.fillMaxWidth()) {
                         Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Camera,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.focalLength, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.focalLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Lens,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(currentItem.aperture, fontFamily = FontFamily.Serif, fontSize = 14.sp)
                                Text(
                                    strings.apertureLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
