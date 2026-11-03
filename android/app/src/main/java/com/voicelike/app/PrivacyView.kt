package com.voicelike.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.Dp

@Composable
fun PrivacyView(
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

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
                text = strings.privacyTitle,
                fontSize = if (LocalAppLanguage.current == AppLanguage.Russian) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = if (LocalAppLanguage.current == AppLanguage.Russian) 18.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        PrivacyContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun PrivacyContent(
    modifier: Modifier = Modifier,
    topSpacing: Dp = 16.dp,
    bottomSpacing: Dp = 100.dp
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(topSpacing))

        PrivacyCard(
            title = strings.privacySection1Title,
            content = strings.privacySection1Content,
            icon = Icons.Outlined.Storage,
            color = Color(0xFFF3F4F6),
            iconColor = Color(0xFF4B5563),
            modifier = Modifier.then(staggeredRevealModifier(0))
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrivacyCard(
            title = strings.privacySection2Title,
            content = strings.privacySection2Content,
            icon = Icons.Outlined.Security,
            color = Color(0xFFDCFCE7),
            iconColor = Color(0xFF16A34A),
            modifier = Modifier.then(staggeredRevealModifier(1))
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrivacyCard(
            title = strings.privacySection3Title,
            content = strings.privacySection3Content,
            icon = Icons.Outlined.Lock,
            color = Color(0xFFE0F2FE),
            iconColor = Color(0xFF0284C7),
            modifier = Modifier.then(staggeredRevealModifier(2))
        )

        Spacer(modifier = Modifier.height(bottomSpacing))
    }
}

@Composable
fun PrivacyCard(
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else color
    val cardIconColor = if (isDark) MaterialTheme.colorScheme.onBackground else iconColor
    val titleColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
    val contentColor = if (isDark) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardColor)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cardIconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = contentColor
        )
    }
}
