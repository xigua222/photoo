package com.voicelike.app

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Deck position as a thin capsule under the "N left" counter. No animation, like iOS.
@Composable
internal fun HomeBatchProgressBar(
    done: Int,
    total: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val fraction = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    val trackWidth = 96.dp
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(onBackground.copy(alpha = 0.10f))
            .semantics { this.contentDescription = contentDescription }
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .width((trackWidth * fraction).coerceAtLeast(4.dp))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// Active range under the counter: the text opens the range panel, the cross resets to all photos.
@Composable
internal fun HomeRangeTag(
    label: String,
    clearLabel: String,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(24.dp)
            .widthIn(max = 260.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.12f), shape)
            .border(0.5.dp, primary.copy(alpha = 0.25f), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(onClick = onOpen)
                .padding(start = 10.dp, end = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onClear)
                .padding(start = 2.dp, end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = clearLabel,
                tint = primary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// The current photo's album, sitting on the range tag's line so the header keeps its height.
@Composable
internal fun HomeCurrentFolderInline(
    name: String,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = "$contentDescription: $name"
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
    }
}
