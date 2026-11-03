package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UnifiedSubpageTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String = LocalAppStrings.current.back,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    backButtonContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    backButtonIconColor: Color = MaterialTheme.colorScheme.onBackground,
    onTitleClick: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val titleInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backContentDescription,
                onClick = onBack,
                containerColor = backButtonContainerColor,
                iconTint = backButtonIconColor
            )
            Spacer(modifier = Modifier.weight(1f))
            if (trailing != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing
                )
            }
        }

        if (titleContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                titleContent()
            }
        } else if (title.isNotBlank()) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clickable(
                                interactionSource = titleInteractionSource,
                                indication = null,
                                onClick = onTitleClick
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@Composable
fun UnifiedSubpageTopBarIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    loading: Boolean = false
) {
    val iconInteractionSource = remember { MutableInteractionSource() }

    val buttonModifier = modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(if (enabled || loading) containerColor else containerColor.copy(alpha = 0.7f))

    if (onLongClick == null) {
        Box(
            modifier = buttonModifier
                .clickable(
                    enabled = enabled && !loading,
                    interactionSource = iconInteractionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = iconTint
                )
            } else {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        Box(
            modifier = buttonModifier.combinedClickable(
                enabled = enabled && !loading,
                onClick = onClick,
                onLongClick = onLongClick
            ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = iconTint
                )
            } else {
                Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
                )
            }
        }
    }
}
