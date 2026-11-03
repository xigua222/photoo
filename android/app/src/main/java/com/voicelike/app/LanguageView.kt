package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LanguageView(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val haptic = LocalHapticFeedback.current
    val hapticsAllowed = LocalHapticsEnabled.current
    val layoutDirection = LocalLayoutDirection.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Colorful backgrounds for languages, cycling through
    val colors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFF45B7D1), // Blue
        Color(0xFF96CEB4), // Green
        Color(0xFFFFBE76), // Orange
        Color(0xFFDFF9FB)  // Light Blue
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, bottom = 24.dp)
        ) {
            UnifiedSubpageTopBarIconButton(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = strings.back,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart),
                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                iconTint = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = strings.selectLanguage,
                fontSize = if (LocalAppLanguage.current == AppLanguage.Russian) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = if (LocalAppLanguage.current == AppLanguage.Russian) 18.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            val sortedLanguages = AppLanguage.values().sortedByDescending { it == currentLanguage }
            
            itemsIndexed(sortedLanguages, key = { _, lang -> lang.name }) { index, lang ->
                val isSelected = lang == currentLanguage
                val color = colors[index % colors.size]
                val rowColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else color
                
                // Optimize animation: Only stagger first few items
                val animIndex = if (index < 8) index else 1
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateItem()
                        .clip(RoundedCornerShape(24.dp))
                        .background(rowColor)
                        .clickable { 
                            if (hapticsAllowed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLanguageSelected(lang) 
                        }
                        .then(staggeredRevealModifier(animIndex))
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang.displayName,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                }
            }
        }
    }
}
