package com.voicelike.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_WIDTH = 90.dp
private val HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_HEIGHT = 6.dp

@Composable
internal fun HomeSnappingCarouselTopProgress(
    modifier: Modifier = Modifier,
    progress: Float,
    isDark: Boolean,
    filterSummary: String? = null,
    currentFolderLabel: String? = null,
    currentFolderName: String? = null,
    currentFolderSpacing: Dp = 4.dp,
    onClearFilter: (() -> Unit)? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "home_snapping_carousel_top_progress"
    )
    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val fillColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
    }
    val secondaryTextColor = if (isDark) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    } else {
        Color.Black.copy(alpha = 0.58f)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_WIDTH)
                .height(HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_HEIGHT)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .width(HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_WIDTH * animatedProgress)
                    .height(HOME_SNAPPING_CAROUSEL_TOP_PROGRESS_HEIGHT)
                    .clip(RoundedCornerShape(999.dp))
                    .background(fillColor)
            )
        }

        if (!filterSummary.isNullOrBlank() && onClearFilter != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Black.copy(alpha = 0.05f)
                    )
                    .bounceClick(onClick = onClearFilter)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = filterSummary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear",
                    tint = secondaryTextColor,
                    modifier = Modifier.height(12.dp)
                )
            }
            if (!currentFolderName.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                HomeCurrentFolderInline(
                    name = currentFolderName,
                    color = secondaryTextColor,
                    contentDescription = currentFolderLabel.orEmpty().substringBefore(':')
                )
            }
            }
        } else if (!currentFolderLabel.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(currentFolderSpacing))
            Text(
                text = currentFolderLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
