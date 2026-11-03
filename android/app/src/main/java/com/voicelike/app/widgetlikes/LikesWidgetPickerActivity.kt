package com.voicelike.app.widgetlikes

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voicelike.app.MediaItem
import com.voicelike.app.PreferencesManager
import com.voicelike.app.R
import com.voicelike.app.UnifiedSubpageTopBarIconButton
import com.voicelike.app.VoiceLikeTheme
import com.voicelike.app.loadMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LikesWidgetPickerActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        setContent {
            VoiceLikeTheme {
                LikesWidgetPickerScreen(
                    appWidgetId = appWidgetId,
                    onClose = { finish() },
                    onSelected = { item ->
                        LikesWidgetStore.saveSelection(this, appWidgetId, item.id, item.uri.toString())
                        LikesPhotoWidgetProvider.updateWidget(
                            context = this,
                            appWidgetManager = AppWidgetManager.getInstance(this),
                            appWidgetId = appWidgetId
                        )
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LikesWidgetPickerScreen(
    appWidgetId: Int,
    onClose: () -> Unit,
    onSelected: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var isLoading by remember { mutableStateOf(true) }
    var likedPhotos by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedMediaId by remember {
        mutableLongStateOf(LikesWidgetStore.getSelectedMediaId(context, appWidgetId) ?: -1L)
    }

    LaunchedEffect(appWidgetId) {
        isLoading = true
        likedPhotos = withContext(Dispatchers.IO) {
            val likedIds = prefs.likedIds.map { it.toLong() }.toSet()
            if (likedIds.isEmpty()) {
                emptyList()
            } else {
                loadMedia(context)
                    .asSequence()
                    .filter { it.type == "photo" && likedIds.contains(it.id) }
                    .sortedByDescending { it.dateAdded }
                    .toList()
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.widget_likes_picker_title)) },
                navigationIcon = {
                    UnifiedSubpageTopBarIconButton(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(id = R.string.widget_likes_picker_back),
                        onClick = onClose,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(id = R.string.widget_likes_picker_loading))
                    }
                }
            }

            likedPhotos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.widget_likes_picker_empty))
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Text(
                        text = stringResource(id = R.string.widget_likes_picker_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(likedPhotos, key = { it.id }) { item ->
                            val isSelected = selectedMediaId == item.id
                            LikePhotoItem(
                                item = item,
                                selected = isSelected,
                                onClick = {
                                    selectedMediaId = item.id
                                    onSelected(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikePhotoItem(
    item: MediaItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }
    val borderWidth = if (selected) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName.ifBlank { item.id.toString() },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
