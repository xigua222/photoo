package com.voicelike.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RawOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Settings > Organize Again: pick one month, album or kind (or all photos) and put its
// kept / skipped photos back into the organize queue, switching the organize range to it.
// Liked, moved and pending-delete photos never come back (selectReorganizeCandidates).

private enum class ReorganizeDimension { Month, Album, Kind }

private class ReorganizeGroup(
    val key: String,
    val range: MediaFilterCriteria,
    val ids: Set<Long>,
    // Month entries only: yyyyMM; album entries only: bucket name; kind entries only: kind.
    val monthValue: Int = 0,
    val album: String? = null,
    val kind: String? = null
)

private class ReorganizeIndex(
    val all: ReorganizeGroup,
    val months: List<ReorganizeGroup>,
    val albums: List<ReorganizeGroup>,
    val kinds: List<ReorganizeGroup>
)

private class ReorganizePendingConfirm(
    val title: String,
    val ids: Set<Long>,
    val range: MediaFilterCriteria
)

private fun buildReorganizeIndex(
    candidates: List<MediaItem>,
    hiddenFolders: Set<String>
): ReorganizeIndex {
    val all = LinkedHashSet<Long>()
    val months = HashMap<Int, LinkedHashSet<Long>>()
    val albums = HashMap<String, LinkedHashSet<Long>>()
    val kinds = HashMap<String, LinkedHashSet<Long>>()
    val kindOrder = OrganizeKind.panelOrder.filter { it != OrganizeKind.ALL }
    for (item in candidates) {
        if (!all.add(item.id)) continue
        months.getOrPut(mediaMonthValue(item)) { LinkedHashSet() }.add(item.id)
        if (item.bucketName !in hiddenFolders) {
            albums.getOrPut(item.bucketName) { LinkedHashSet() }.add(item.id)
        }
        for (kind in kindOrder) {
            if (mediaMatchesKind(item, kind)) kinds.getOrPut(kind) { LinkedHashSet() }.add(item.id)
        }
    }
    return ReorganizeIndex(
        all = ReorganizeGroup(key = "all", range = MediaFilterCriteria(), ids = all),
        months = months.keys.sortedDescending().map { monthValue ->
            ReorganizeGroup(
                key = "month:$monthValue",
                range = MediaFilterCriteria().withTime(month = monthKeyOf(monthValue)),
                ids = months.getValue(monthValue),
                monthValue = monthValue
            )
        },
        albums = albums.entries
            .sortedWith(compareByDescending<Map.Entry<String, Set<Long>>> { it.value.size }.thenBy { it.key })
            .map { (name, ids) ->
                ReorganizeGroup(
                    key = "album:$name",
                    range = MediaFilterCriteria(folder = name),
                    ids = ids,
                    album = name
                )
            },
        kinds = kindOrder.mapNotNull { kind ->
            kinds[kind]?.let { ids ->
                ReorganizeGroup(
                    key = "kind:$kind",
                    range = MediaFilterCriteria(type = kind),
                    ids = ids,
                    kind = kind
                )
            }
        }
    )
}

private fun reorganizeLocale(language: AppLanguage): Locale = when (language) {
    AppLanguage.English -> Locale.ENGLISH
    AppLanguage.SimplifiedChinese -> Locale.SIMPLIFIED_CHINESE
    AppLanguage.TraditionalChinese -> Locale.TRADITIONAL_CHINESE
    AppLanguage.Korean -> Locale.KOREAN
    AppLanguage.Japanese -> Locale.JAPANESE
    AppLanguage.French -> Locale.FRENCH
    AppLanguage.German -> Locale.GERMAN
    AppLanguage.Italian -> Locale.ITALIAN
    AppLanguage.Portuguese -> Locale("pt")
    AppLanguage.Spanish -> Locale("es")
    AppLanguage.Russian -> Locale("ru")
    AppLanguage.Thai -> Locale("th")
    AppLanguage.Vietnamese -> Locale("vi")
    AppLanguage.Indonesian -> Locale("id")
    AppLanguage.Arabic -> Locale("ar")
    else -> language.locale
}

/** "2026年10月" for Chinese / Japanese, "2026년 10월" for Korean, otherwise "Oct 2026" style. */
internal fun reorganizeMonthTitle(monthValue: Int, language: AppLanguage): String {
    val year = monthValue / 100
    val month = monthValue % 100
    return when (language) {
        AppLanguage.SimplifiedChinese,
        AppLanguage.TraditionalChinese,
        AppLanguage.Japanese -> "%d年%d月".format(Locale.US, year, month)
        AppLanguage.Korean -> "%d년 %d월".format(Locale.US, year, month)
        else -> {
            if (month !in 1..12) return year.toString()
            val locale = reorganizeLocale(language)
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMyyyy")
            val calendar = Calendar.getInstance().apply {
                clear()
                set(year, month - 1, 1)
            }
            SimpleDateFormat(pattern, locale)
                .format(calendar.time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }
}

/** Toast after Organize Again put photos back. */
internal fun reorganizeDoneToast(count: Int, language: AppLanguage): String =
    organizeResultStrings(language).reorganizeDoneFmt.format(count)

private fun reorganizeKindIcon(kind: String): ImageVector = when (kind) {
    OrganizeKind.PHOTO -> Icons.Outlined.Image
    OrganizeKind.LIVE -> Icons.Outlined.MotionPhotosOn
    OrganizeKind.VIDEO -> Icons.Outlined.Videocam
    OrganizeKind.SCREENSHOT -> Icons.Outlined.Screenshot
    OrganizeKind.SCREEN_RECORDING -> Icons.Outlined.RadioButtonChecked
    OrganizeKind.PANORAMA -> Icons.Outlined.Panorama
    OrganizeKind.ANIMATED -> Icons.Outlined.Animation
    OrganizeKind.RAW -> Icons.Outlined.RawOn
    else -> Icons.Outlined.GridView
}

@Composable
internal fun ReorganizePage(
    candidates: List<MediaItem>,
    hiddenFolders: Set<String>,
    onBack: () -> Unit,
    onConfirm: (ids: Set<Long>, range: MediaFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val copy = remember(language) { organizeResultStrings(language) }
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var dimension by rememberSaveable { mutableStateOf(ReorganizeDimension.Month) }
    var pendingConfirm by remember { mutableStateOf<ReorganizePendingConfirm?>(null) }

    val index by produceState<ReorganizeIndex?>(initialValue = null, candidates, hiddenFolders) {
        value = withContext(Dispatchers.Default) { buildReorganizeIndex(candidates, hiddenFolders) }
    }

    BackHandler(onBack = onBack)

    fun requestConfirm(title: String, group: ReorganizeGroup) {
        if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pendingConfirm = ReorganizePendingConfirm(title = title, ids = group.ids, range = group.range)
    }

    fun groupTitle(group: ReorganizeGroup): String = when {
        group.kind != null -> copy.kindTitle(group.kind)
        group.album != null -> group.album.ifBlank { copy.unnamedAlbum }
        group.key == "all" -> copy.allPhotos
        else -> reorganizeMonthTitle(group.monthValue, language)
    }

    fun groupIcon(group: ReorganizeGroup): ImageVector = when {
        group.kind != null -> reorganizeKindIcon(group.kind)
        group.album != null -> Icons.Outlined.Folder
        group.key == "all" -> Icons.Outlined.GridView
        else -> Icons.Outlined.CalendarMonth
    }

    pendingConfirm?.let { confirm ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text(text = copy.confirmTitleFmt.format(confirm.title)) },
            text = { Text(text = copy.confirmBodyFmt.format(confirm.ids.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConfirm = null
                        onConfirm(confirm.ids, confirm.range)
                    }
                ) {
                    Text(
                        text = copy.confirmActionFmt.format(confirm.ids.size),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) {
                    Text(
                        text = copy.cancel,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            UnifiedSubpageTopBar(
                title = copy.reorganizeTitle,
                onBack = onBack
            )

            val currentIndex = index
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    currentIndex == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    currentIndex.all.ids.isEmpty() -> ReorganizeEmptyState(text = copy.reorganizeEmpty, isDark = isDark)
                    else -> {
                        ReorganizeGroupCard(isDark = isDark) {
                            ReorganizeRow(
                                title = copy.allPhotos,
                                countText = copy.photosCountFmt.format(currentIndex.all.ids.size),
                                icon = Icons.Outlined.GridView,
                                onClick = { requestConfirm(copy.allPhotos, currentIndex.all) }
                            )
                        }

                        ReorganizeSegmentedControl(
                            selected = dimension,
                            labels = listOf(
                                ReorganizeDimension.Month to copy.tabMonth,
                                ReorganizeDimension.Album to copy.tabAlbum,
                                ReorganizeDimension.Kind to copy.tabType
                            ),
                            isDark = isDark,
                            onSelect = { next ->
                                if (next != dimension) {
                                    if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dimension = next
                                }
                            }
                        )

                        AnimatedContent(
                            targetState = dimension,
                            transitionSpec = {
                                (fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(120)))
                                    .using(SizeTransform(clip = false) { _, _ -> tween(240) })
                            },
                            label = "reorganizeDimension"
                        ) { shownDimension ->
                            val groups = when (shownDimension) {
                                ReorganizeDimension.Month -> currentIndex.months
                                ReorganizeDimension.Album -> currentIndex.albums
                                ReorganizeDimension.Kind -> currentIndex.kinds
                            }
                            if (groups.isEmpty()) {
                                ReorganizeGroupCard(isDark = isDark) {
                                    Text(
                                        text = copy.reorganizeEmpty,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 28.dp)
                                    )
                                }
                            } else {
                                ReorganizeGroupCard(isDark = isDark) {
                                    groups.forEachIndexed { position, group ->
                                        if (position > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 50.dp)
                                                    .fillMaxWidth()
                                                    .height(0.5.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.1f else 0.07f)
                                                    )
                                            )
                                        }
                                        val title = groupTitle(group)
                                        ReorganizeRow(
                                            title = title,
                                            countText = copy.photosCountFmt.format(group.ids.size),
                                            icon = groupIcon(group),
                                            onClick = { requestConfirm(title, group) }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = copy.reorganizeNote,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorganizeGroupCard(
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.14f else 0.1f),
                shape = shape
            ),
        content = content
    )
}

@Composable
private fun ReorganizeRow(
    title: String,
    countText: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = countText,
            fontSize = 14.sp,
            color = muted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ReorganizeSegmentedControl(
    selected: ReorganizeDimension,
    labels: List<Pair<ReorganizeDimension, String>>,
    isDark: Boolean,
    onSelect: (ReorganizeDimension) -> Unit
) {
    val trackShape = RoundedCornerShape(12.dp)
    val segmentShape = RoundedCornerShape(9.dp)
    val gap = 3.dp
    val selectedIndex = labels.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.1f else 0.05f))
            .padding(3.dp)
    ) {
        // One thumb slides under the labels instead of each segment fading its own background.
        val segmentWidth = (maxWidth - gap * (labels.size - 1)) / labels.size
        val thumbOffset by animateDpAsState(
            targetValue = (segmentWidth + gap) * selectedIndex,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            label = "reorganizeSegmentThumb"
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .height(34.dp)
                .then(if (isDark) Modifier else Modifier.shadow(1.5.dp, segmentShape, clip = false))
                .clip(segmentShape)
                .background(if (isDark) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            labels.forEach { (value, label) ->
                val isSelected = value == selected
                val textColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 1f else 0.6f),
                    animationSpec = tween(180),
                    label = "reorganizeSegmentText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorganizeEmptyState(text: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
        )
    }
}
