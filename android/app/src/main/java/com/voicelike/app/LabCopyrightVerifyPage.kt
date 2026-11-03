package com.voicelike.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun LabCopyrightVerifyPage(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val copy = remember(appLanguage) { getWatermarkExtraCopy(appLanguage) }
    val scope = rememberCoroutineScope()
    var selectedUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var verifyResult by remember { mutableStateOf<WatermarkCopyrightVerifyResult?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedUriText = uri.toString()
        verifyResult = null
        errorMessage = null
        scope.launch {
            isWorking = true
            runCatching {
                verifyWatermarkCopyright(context, uri)
            }.onSuccess { result ->
                verifyResult = result
            }.onFailure { error ->
                errorMessage = error.message ?: copy.verifyFailed
            }
            isWorking = false
        }
    }

    val selectedUri = selectedUriText?.let(android.net.Uri::parse)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnifiedSubpageTopBar(
            title = copy.copyrightVerifyTitle,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = copy.copyrightVerifySubtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                lineHeight = 18.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { picker.launch("image/*") },
                    enabled = !isWorking,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.chooseImage)
                }
                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        scope.launch {
                            isWorking = true
                            errorMessage = null
                            runCatching {
                                verifyWatermarkCopyright(context, uri)
                            }.onSuccess { result ->
                                verifyResult = result
                            }.onFailure { error ->
                                errorMessage = error.message ?: copy.verifyFailed
                            }
                            isWorking = false
                        }
                    },
                    enabled = selectedUri != null && !isWorking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.reverify)
                }
            }

            if (selectedUri != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF1F1F1F) else Color(0xFFF3F6FA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(selectedUri).crossfade(true).build(),
                        contentDescription = copy.pendingVerifyImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                    )
                }
            }

            if (isWorking) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.4.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(copy.verifying, fontSize = 12.sp)
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626)
                )
            }

            verifyResult?.let { result ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (MaterialTheme.colorScheme.background == Color.Black) Color(0xFF1F1F1F) else Color(0xFFF3F6FA),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${copy.combinedConfidence} ${(result.confidence * 100f).toInt()}%",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(result.conclusion, fontSize = 12.sp)
                        Text("${copy.creatorField}：${result.creatorSummary}", fontSize = 12.sp)
                        Text("${copy.copyrightField}：${result.rightsSummary}", fontSize = 12.sp)
                        Text("${copy.metadataDeclaration}：${if (result.metadataDetected) copy.detected else copy.notDetected}", fontSize = 12.sp)
                        Text("${copy.noAiDeclaration}：${if (result.noAiDeclarationDetected) copy.detected else copy.notDetected}", fontSize = 12.sp)
                        Text(
                            "${copy.invisibleWatermarkConfidence} ${(result.invisibleWatermarkConfidence * 100f).toInt()}%",
                            fontSize = 12.sp
                        )
                        Text(
                            "${copy.localFingerprint}：${
                                if (result.localFingerprintMatched) {
                                    "${copy.matched}（距离 ${result.localFingerprintDistance}）"
                                } else {
                                    copy.notMatched
                                }
                            }",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
