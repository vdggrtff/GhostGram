package com.ghostgram.app.presentation.components.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghostgram.app.Platform
import com.ghostgram.app.getPlatform
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.TimeFormatter
import com.ghostgram.app.utils.openVideoInSystemPlayer
import com.ghostgram.app.utils.saveMediaToDownloads
import entity.Message
import entity.MessageMediaType
import kotlinx.coroutines.launch

@Composable
fun GhostMediaViewer(
    mediaMessages: List<Message>,
    initialMessageId: Long,
    onDismiss: () -> Unit
) {
    if (mediaMessages.isEmpty()) return

    val initialIndex = mediaMessages.indexOfFirst { it.id == initialMessageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaMessages.size })
    val coroutineScope = rememberCoroutineScope()
    var isHudVisible by remember { mutableStateOf(true) }
    var saveStatusText by remember { mutableStateOf<String?>(null) }

    val currentMessage = mediaMessages.getOrNull(pagerState.currentPage) ?: mediaMessages.first()
    val isCurrentVideo = currentMessage.mediaType == MessageMediaType.VIDEO

    // Фокус для перехвата клавиш на ПК
    val focusRequester = remember { FocusRequester() }

    val isDesktopPlatform = remember { !getPlatform().name.startsWith("Android") }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                // 💥 ПЕРЕХВАТ КЛАВИАТУРЫ (СТРЕЛКИ И ESC НА ПК)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionLeft -> {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    true
                                } else false
                            }
                            Key.DirectionRight -> {
                                if (pagerState.currentPage < mediaMessages.size - 1) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    true
                                } else false
                            }
                            Key.Escape -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            // ПЕЙДЖЕР
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val message = mediaMessages[page]
                val model = message.photoPath ?: message.fileName

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (model != null) {
                        ZoomableImage(
                            model = model,
                            contentDescription = "Media $page",
                            onTap = { isHudVisible = !isHudVisible },
                            onDismiss = onDismiss
                        )

                        if (message.mediaType == MessageMediaType.VIDEO && !message.fileName.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { openVideoInSystemPlayer(message.fileName!!) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = GhostPrimary)
                    }
                }
            }

            // 💥 БОКОВЫЕ СТРЕЛКИ ДЛЯ ПК (ЛЕВАЯ И ПРАВАЯ)
            if (isDesktopPlatform && isHudVisible) {
                // Стрелка «Назад» (Влево)
                if (pagerState.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущее", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Стрелка «Вперед» (Вправо)
                if (pagerState.currentPage < mediaMessages.size - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующее", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ВЕРХНИЙ HUD
            AnimatedVisibility(
                visible = isHudVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pagerState.currentPage + 1} из ${mediaMessages.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentMessage.senderName.ifBlank { "Медиа" }} • ${TimeFormatter.formatTime(currentMessage.date)}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // НИЖНИЙ HUD
            AnimatedVisibility(
                visible = isHudVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (saveStatusText != null) {
                        Text(
                            text = saveStatusText!!,
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val sourcePath = currentMessage.fileName ?: currentMessage.photoPath
                                if (!sourcePath.isNullOrBlank()) {
                                    val ext = if (isCurrentVideo) "mp4" else "jpg"
                                    val fileName = "ghostgram_${currentMessage.id}.$ext"
                                    val savedPath = saveMediaToDownloads(sourcePath, fileName)
                                    saveStatusText = if (savedPath != null) "✓ Сохранено в Загрузки" else "❌ Ошибка сохранения"
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Скачать", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }
    }
}