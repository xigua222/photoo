package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DynamicLiveBadge(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    label: String = "LIVE"
) {
    Box(
        modifier = modifier
            .background(
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.84f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black
        )
    }
}
