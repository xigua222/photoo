package com.voicelike.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShootingStatsEasterEgg(
    isDark: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    canGoPrev: Boolean = true
) {
    val newPageStrings = getNewPageStrings(LocalAppLanguage.current)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(staggeredRevealModifier(2))
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF7ED))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1826",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF9A3412),
                    lineHeight = 40.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = newPageStrings.statsEasterEggBody,
                fontSize = 14.sp,
                color = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color(0xFFC2410C).copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 0.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.le_gras_1826),
                contentDescription = "View from the Window at Le Gras",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .aspectRatio(1.5f), // Adjust aspect ratio based on image
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Prev",
                        tint = if (isDark) MaterialTheme.colorScheme.onBackground else Color(0xFF9A3412)
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (isDark) {
                            if (canGoNext) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        } else {
                            if (canGoNext) Color(0xFF9A3412) else Color(0xFF9A3412).copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }
    }
}
