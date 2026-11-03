package com.voicelike.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LikesCommentSheet(
    item: MediaItem?,
    visible: Boolean,
    prefs: PreferencesManager,
    onDismiss: () -> Unit,
    onCommentsChanged: () -> Unit = {}
) {
    if (item == null) return

    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val itemId = item.id
    var comments by remember(itemId) { mutableStateOf(prefs.comments[itemId] ?: emptyList()) }
    var input by remember(itemId) { mutableStateOf(TextFieldValue("")) }
    var showDeleteCommentDialog by remember(itemId) { mutableStateOf(false) }
    var commentToDeleteIndex by remember(itemId) { mutableIntStateOf(-1) }
    var expandedCommentIndex by remember(itemId) { mutableIntStateOf(-1) }
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.62f

    fun persistComments(newComments: List<String>) {
        comments = newComments
        val allComments = prefs.comments
        allComments[itemId] = newComments
        prefs.comments = allComments
        onCommentsChanged()
    }

    fun closeCommentSheet() {
        scope.launch {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            showDeleteCommentDialog = false
            commentToDeleteIndex = -1
            expandedCommentIndex = -1
            onDismiss()
        }
    }

    LaunchedEffect(visible, itemId) {
        if (visible && !sheetState.isVisible) {
            sheetState.show()
        }
    }

    if (!visible) return

    BackHandler(enabled = true) {
        if (imeVisible) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        } else {
            closeCommentSheet()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { closeCommentSheet() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            Text(
                text = "${comments.size} ${strings.commentsHeader}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(comments) { index, comment ->
                    val isCommentMenuExpanded = expandedCommentIndex == index
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCommentMenuExpanded) {
                                    if (isDark) {
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                                    } else {
                                        Color.Black.copy(alpha = 0.06f)
                                    }
                                } else {
                                    Color.Transparent
                                }
                            )
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    expandedCommentIndex = index
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context = context)
                                    .data(resolveUserAvatarModel(prefs.avatarType, prefs.customAvatarPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.commentUserMe,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = comment,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        MaterialTheme(
                            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                        ) {
                            val menuBgColor = MaterialTheme.colorScheme.surface
                            val menuTextColor = MaterialTheme.colorScheme.onSurface
                            DropdownMenu(
                                expanded = isCommentMenuExpanded,
                                onDismissRequest = { expandedCommentIndex = -1 },
                                modifier = Modifier
                                    .background(menuBgColor)
                                    .width(180.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.copy, color = menuTextColor) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(comment))
                                        Toast.makeText(context, strings.commentCopied, Toast.LENGTH_SHORT).show()
                                        expandedCommentIndex = -1
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            strings.delete,
                                            color = if (isDark) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                                        )
                                    },
                                    onClick = {
                                        commentToDeleteIndex = index
                                        showDeleteCommentDialog = true
                                        expandedCommentIndex = -1
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showDeleteCommentDialog && commentToDeleteIndex != -1) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteCommentDialog = false
                        commentToDeleteIndex = -1
                    },
                    title = { Text(strings.deleteCommentTitle) },
                    text = { Text(strings.deleteCommentMessage) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (commentToDeleteIndex in comments.indices) {
                                    val newComments = comments.toMutableList()
                                    newComments.removeAt(commentToDeleteIndex)
                                    persistComments(newComments)
                                }
                                showDeleteCommentDialog = false
                                commentToDeleteIndex = -1
                            }
                        ) {
                            Text(strings.delete, color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteCommentDialog = false
                                commentToDeleteIndex = -1
                            }
                        ) {
                            Text(strings.cancel, color = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    textContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isDark) 0.18f else 0.10f)
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp)
                    .advancedTextureCard(shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.9f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text(strings.commentPlaceholder) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    if (input.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val newComment = input.text.trim()
                                if (newComment.isNotEmpty()) {
                                    persistComments(comments + newComment)
                                    input = TextFieldValue("")
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Send, strings.send, tint = Color(0xFFFE2C55))
                        }
                    }
                }
            }
        }
    }
}
