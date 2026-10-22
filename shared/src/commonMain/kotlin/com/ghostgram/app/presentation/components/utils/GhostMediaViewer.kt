package com.ghostgram.app.presentation.components.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import com.ghostgram.app.getPlatform
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.utils.TimeFormatter
import com.ghostgram.app.utils.openVideoInSystemPlayer
import com.ghostgram.app.utils.saveMediaToDownloads
import com.ghostgram.app.utils.video.InAppVideoPlayer
import com.ghostgram.app.utils.video.MediaViewerSystemBars
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

    val initialIndex = mediaMessages
        .indexOfFirst { it.id == initialMessageId }
        .coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaMessages.size }
    )

    var isCurrentMediaZoomed by remember(pagerState.currentPage) {
        mutableStateOf(false)
    }

    var isTransformGestureActive by remember(pagerState.currentPage) {
        mutableStateOf(false)
    }

    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapPositionalThreshold = 0.75f
    )

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val isDesktopPlatform = remember {
        !getPlatform().name.startsWith("Android")
    }

    var isHudVisible by remember { mutableStateOf(true) }
    var saveStatusText by remember { mutableStateOf<String?>(null) }


    val currentMessage =
        mediaMessages.getOrNull(pagerState.currentPage)
            ?: mediaMessages.first()

    val isCurrentVideo =
        currentMessage.mediaType == MessageMediaType.VIDEO

    // Автоматически скрываем HUD во время просмотра видео.
    LaunchedEffect(
        pagerState.currentPage,
        isHudVisible,
        isCurrentVideo,
        isDesktopPlatform
    ) {
        if (
            isCurrentVideo &&
            !isDesktopPlatform &&
            isHudVisible
        ) {
            kotlinx.coroutines.delay(3500)
            isHudVisible = false
        }
    }

    LaunchedEffect(Unit) {
        if (isDesktopPlatform) {
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // Android actual скрывает системные панели на время просмотра.
        MediaViewerSystemBars()

        // Неподвижный чёрный фон.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Этот слой перемещается при свайпе вверх/вниз.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onKeyEvent false
                        }

                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage - 1
                                        )
                                    }
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.DirectionRight -> {
                                if (
                                    pagerState.currentPage <
                                    mediaMessages.lastIndex
                                ) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage + 1
                                        )
                                    }
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.Escape -> {
                                onDismiss()
                                true
                            }

                            else -> false
                        }
                    }
            ) {
                // -------------------------------------------------
                // МЕДИА: ФОТОГРАФИИ И ВИДЕО
                // -------------------------------------------------

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isCurrentMediaZoomed &&
                            !isTransformGestureActive,
                    flingBehavior = pagerFlingBehavior
                ) { page ->
                    val message = mediaMessages[page]

                    val isVideo =
                        message.mediaType == MessageMediaType.VIDEO

                    // У видео photoPath используется как превью.
                    // Не пытаемся декодировать MP4 как изображение.
                    val previewModel = if (isVideo) {
                        message.photoPath
                    } else {
                        message.photoPath ?: message.fileName
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // Активное видео на Android.
                            isVideo &&
                                    page == pagerState.currentPage &&
                                    !isDesktopPlatform &&
                                    !message.fileName.isNullOrBlank() -> {

                                InAppVideoPlayer(
                                    videoPath = message.fileName!!,
                                    controlsVisible = isHudVisible,
                                    onToggleControls = {
                                        isHudVisible = !isHudVisible
                                    },
                                    onDismiss = onDismiss
                                )
                            }

                            // Фотография или превью видео.
                            previewModel != null -> {
                                ZoomableImage(
                                    model = previewModel,
                                    contentDescription = "Media $page",
                                    onTap = {
                                        isHudVisible = !isHudVisible
                                    },
                                    onDismiss = onDismiss,
                                    onZoomChanged = { isZoomed ->
                                        isCurrentMediaZoomed = isZoomed
                                    },
                                    onTransformGestureChanged = { isGestureActive ->
                                        isTransformGestureActive = isGestureActive
                                    }
                                )
                            }

                            // Нет ни изображения, ни превью.
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isVideo) {
                                        CircularProgressIndicator(
                                            color = GhostPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // На Desktop сохраняем открытие системного плеера.
                        // Android воспроизводит видео встроенно.
                        if (
                            isVideo &&
                            isDesktopPlatform &&
                            !message.fileName.isNullOrBlank()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                    .clickable {
                                        openVideoInSystemPlayer(
                                            message.fileName!!
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Воспроизвести",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------
                // БОКОВЫЕ СТРЕЛКИ ДЛЯ DESKTOP
                // -------------------------------------------------

                if (isDesktopPlatform && isHudVisible) {
                    if (pagerState.currentPage > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Color.Black.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage - 1
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Предыдущее",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (
                        pagerState.currentPage <
                        mediaMessages.lastIndex
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Color.Black.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage + 1
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Следующее",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // -------------------------------------------------
                // ВЕРХНЯЯ ПАНЕЛЬ
                // -------------------------------------------------

                AnimatedVisibility(
                    // 💥 ПРЯЧЕМ ХУД, ЕСЛИ ФОТО УВЕЛИЧЕНО!
                    visible = isHudVisible && !isCurrentMediaZoomed,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                // 💥 Легкий мягкий градиент вместо черной грязи:
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .statusBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} из ${mediaMessages.size}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "${currentMessage.senderName.ifBlank { "Медиа" }} • ${
                                    TimeFormatter.formatTime(currentMessage.date)
                                }",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val sourcePath = currentMessage.fileName ?: currentMessage.photoPath
                                if (!sourcePath.isNullOrBlank()) {
                                    val extension = if (isCurrentVideo) "mp4" else "jpg"
                                    val fileName = "ghostgram_${currentMessage.id}.$extension"
                                    val savedPath = saveMediaToDownloads(sourcePath, fileName)
                                    saveStatusText = if (savedPath != null) "✓ Сохранено в Загрузки" else "❌ Ошибка сохранения"
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Скачать",
                                tint = Color.White
                            )
                        }
                    }
                }

                // -------------------------------------------------
                // СТАТУС СОХРАНЕНИЯ
                // -------------------------------------------------

                AnimatedVisibility(
                    visible = isHudVisible &&
                            saveStatusText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = saveStatusText.orEmpty(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 20.dp
                            )
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            )
                    )
                }
            }
        }
    }
}
