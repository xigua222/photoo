package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LikesTutorialDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = newPageStrings.likesTutorialTitle,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = newPageStrings.likesTutorialIntro,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                    lineHeight = 19.sp
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f))
                Text(
                    text = newPageStrings.likesTutorialActionsTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                TutorialRow(
                    icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color(0xFFFF8A9D)) },
                    text = newPageStrings.likesTutorialComments,
                    isDark = isDark
                )
                TutorialRow(
                    icon = { Icon(Icons.Outlined.Share, contentDescription = null, tint = Color(0xFF7DD3FC)) },
                    text = newPageStrings.likesTutorialShare,
                    isDark = isDark
                )
                TutorialRow(
                    icon = { Icon(Icons.Outlined.Notes, contentDescription = null, tint = Color(0xFFA7F3D0)) },
                    text = newPageStrings.likesTutorialDanmaku,
                    isDark = isDark
                )
                TutorialRow(
                    icon = { Icon(Icons.Outlined.HeartBroken, contentDescription = null, tint = Color(0xFFFCA5A5)) },
                    text = newPageStrings.likesTutorialRemoveFavorite,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = newPageStrings.likesTutorialTip,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(newPageStrings.likesTutorialGotIt)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun TutorialRow(
    icon: @Composable () -> Unit,
    text: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}
