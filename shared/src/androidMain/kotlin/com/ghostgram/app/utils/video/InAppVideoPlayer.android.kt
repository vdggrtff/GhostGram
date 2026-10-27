package com.ghostgram.app.utils.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
actual fun InAppVideoPlayer(
    videoPath: String,
    controlsVisible: Boolean,
    playbackSpeed: Float,
    isLooping: Boolean,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onTransformGestureChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val cleanPath = remember(videoPath) {
        if (videoPath.startsWith("file://")) videoPath.removePrefix("file://") else videoPath
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }

    // 💥 СТЕЙТЫ ЗУМА И СВАЙПА ЗАКРЫТИЯ
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val dismissOffsetYAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Плашка всплывающей перемотки (±10 сек)
    var seekTooltipText by remember { mutableStateOf<String?>(null) }
    var seekTooltipAlignment by remember { mutableStateOf(Alignment.Center) }

    val exoPlayer = remember(cleanPath) {
        ExoPlayer.Builder(context).build().apply {
            val file = File(cleanPath)
            if (file.exists()) {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
                prepare()
                playWhenReady = true
            }
        }
    }

    // Скорость и зацикливание
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }
    LaunchedEffect(isLooping, durationMs) {
        // Короткие видео до 15 сек крутим по кругу автоматически, длинные — по кнопке
        val shouldLoop = isLooping || (durationMs in 1L..15_000L)
        exoPlayer.repeatMode = if (shouldLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) {
                    isEnded = true
                    isPlaying = false
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) isEnded = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Таймер позиции
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isActive) {
            if (isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            delay(50)
        }
    }

    // 💥 ФУНКЦИЯ PLAY / REPLAY (С рестартом с 00:00 если закончилось!)
    fun handlePlayPause() {
        if (isEnded || (durationMs > 0L && currentPositionMs >= durationMs - 300L)) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            isEnded = false
        } else {
            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
    }

    // 💥 ФУНКЦИЯ ПЕРЕМОТКИ НА ±10 СЕКУНД
    fun seekRelative(seconds: Int, isForward: Boolean) {
        val target = if (isForward) {
            (exoPlayer.currentPosition + (seconds * 1000L)).coerceAtMost(durationMs)
        } else {
            (exoPlayer.currentPosition - (seconds * 1000L)).coerceAtLeast(0L)
        }
        exoPlayer.seekTo(target)
        currentPositionMs = target

        // Показываем плашку перемотки
        seekTooltipText = if (isForward) "$seconds сек ⏩" else "⏪ $seconds сек"
        seekTooltipAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
        coroutineScope.launch {
            delay(650)
            seekTooltipText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 💥 1. ЖЕСТЫ ТАПА: ОДИНОЧНЫЙ = ХУД, ДВОЙНОЙ СЛЕВА = -10с, СПРАВА = +10с, ЦЕНТР = ЗУМ!
            .pointerInput(cleanPath) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { tapPos ->
                        val screenWidth = size.width
                        when {
                            // Левая треть экрана: -10 сек
                            tapPos.x < screenWidth * 0.35f -> seekRelative(10, isForward = false)
                            // Правая треть экрана: +10 сек
                            tapPos.x > screenWidth * 0.65f -> seekRelative(10, isForward = true)
                            // Центр экрана: Зум 2.5x / Сброс в 1x
                            else -> {
                                coroutineScope.launch {
                                    if (scaleAnim.value > 1.05f) {
                                        onZoomChanged(false)
                                        launch { scaleAnim.animateTo(1f, tween(200)) }
                                        launch { offsetXAnim.animateTo(0f, tween(200)) }
                                        launch { offsetYAnim.animateTo(0f, tween(200)) }
                                    } else {
                                        onZoomChanged(true)
                                        launch { scaleAnim.animateTo(2.5f, tween(200)) }
                                    }
                                }
                            }
                        }
                    }
                )
            }
            // 💥 2. ПИНЧ-ТУ-ЗУМ 2 ПАЛЬЦАМИ + СВАЙП ВВЕРХ/ВНИЗ ДЛЯ ВЫХОДА
            .pointerInput(cleanPath) {
                awaitEachGesture {
                    var isPinching = false
                    var isVerticalDrag = false
                    var totalDragY = 0f
                    var totalDragX = 0f

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointers = event.changes
                        val pressedPointers = pointers.filter { it.pressed }

                        // 💥 2 ПАЛЬЦА: СВОБОДНЫЙ ЗУМ ВИДЕО ДО 4X!
                        if (pressedPointers.size >= 2) {
                            if (!isPinching) {
                                isPinching = true
                                onTransformGestureChanged(true)
                            }
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scaleAnim.value * zoom).coerceIn(1f, 4.5f)

                            coroutineScope.launch { scaleAnim.snapTo(newScale) }
                            onZoomChanged(newScale > 1.05f)

                            if (newScale > 1.05f) {
                                val maxOffsetX = size.width * (newScale - 1f) / 2f
                                val maxOffsetY = size.height * (newScale - 1f) / 2f
                                coroutineScope.launch {
                                    offsetXAnim.snapTo((offsetXAnim.value + pan.x).coerceIn(-maxOffsetX, maxOffsetX))
                                    offsetYAnim.snapTo((offsetYAnim.value + pan.y).coerceIn(-maxOffsetY, maxOffsetY))
                                }
                            }
                            pointers.forEach { it.consume() }
                        }
                        // 💥 1 ПАЛЕЦ: ПАНОРАМИРОВАНИЕ ИЛИ СВАЙП ВЫХОДА
                        else if (pressedPointers.size == 1 && !isPinching) {
                            val change = pressedPointers.first()
                            val pan = change.positionChange()

                            if (scaleAnim.value > 1.05f) {
                                val maxOffsetX = size.width * (scaleAnim.value - 1f) / 2f
                                val maxOffsetY = size.height * (scaleAnim.value - 1f) / 2f
                                coroutineScope.launch {
                                    offsetXAnim.snapTo((offsetXAnim.value + pan.x).coerceIn(-maxOffsetX, maxOffsetX))
                                    offsetYAnim.snapTo((offsetYAnim.value + pan.y).coerceIn(-maxOffsetY, maxOffsetY))
                                }
                                change.consume()
                            } else {
                                // Масштаб 1х: свайп вверх/вниз для закрытия
                                totalDragY += pan.y
                                totalDragX += pan.x

                                if (!isVerticalDrag && abs(totalDragY) > 8f && abs(totalDragY) > abs(totalDragX) * 1.3f) {
                                    isVerticalDrag = true
                                }

                                if (isVerticalDrag) {
                                    coroutineScope.launch {
                                        dismissOffsetYAnim.snapTo(dismissOffsetYAnim.value + pan.y)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    onTransformGestureChanged(false)

                    // Отпустили палец при свайпе выхода:
                    if (scaleAnim.value <= 1.05f) {
                        if (abs(dismissOffsetYAnim.value) > 160f) {
                            onDismiss() // 💥 ВИДЕО ЗАКРЫВАЕТСЯ ПРИ СВАЙПЕ!
                        } else {
                            coroutineScope.launch { dismissOffsetYAnim.animateTo(0f, tween(150)) }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // САМ ВИДЕОПЛЕЕР С ТРАНСФОРМАЦИЕЙ
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value + dismissOffsetYAnim.value
                    alpha = (1f - (abs(dismissOffsetYAnim.value) / 500f)).coerceIn(0.2f, 1f)
                }
        )

        // Крутилка буферизации
        if (isBuffering) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
        }

        // 💥 ВСПЛЫВАЮЩАЯ ПЛАШКА ПЕРЕМОТКИ (±10 СЕК)
        AnimatedVisibility(
            visible = seekTooltipText != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(seekTooltipAlignment)
                .padding(horizontal = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = seekTooltipText.orEmpty(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 💥 КОНТРОЛЫ (ХУД)
        if (controlsVisible && scaleAnim.value <= 1.05f) {
            // КНОПКА ПО ЦЕНТРУ (Play / Pause / Replay)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { handlePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying && !isEnded) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // ТАЙМЛАЙН
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubFraction by remember { mutableStateOf<Float?>(null) }
                var trackWidthPx by remember { mutableStateOf(1f) }

                val animatedTrackHeight by animateDpAsState(
                    targetValue = if (isScrubbing) 6.dp else 2.5.dp,
                    animationSpec = tween(150),
                    label = "trackHeight"
                )
                val animatedThumbRadius by animateDpAsState(
                    targetValue = if (isScrubbing) 8.dp else 3.5.dp,
                    animationSpec = tween(150),
                    label = "thumbRadius"
                )

                val progress = if (durationMs > 0L) {
                    scrubFraction ?: (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val previewTimeMs = (progress * durationMs).toLong()

                // Мини-экранчик с таймкодом
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    if (isScrubbing && durationMs > 0L) {
                        val tooltipWidth = 60.dp
                        val density = LocalDensity.current
                        val tooltipWidthPx = with(density) { tooltipWidth.toPx() }

                        val rawOffset = (trackWidthPx * progress) - (tooltipWidthPx / 2f)
                        val clampedOffset = rawOffset.coerceIn(0f, (trackWidthPx - tooltipWidthPx).coerceAtLeast(0f))
                        val offsetDp = with(density) { clampedOffset.toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = offsetDp, y = 0.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatTimeMs(previewTimeMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Полоска
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(durationMs) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (durationMs <= 0L) return@awaitEachGesture

                                isScrubbing = true
                                var curX = down.position.x
                                val initialFrac = (curX / size.width).coerceIn(0f, 1f)
                                scrubFraction = initialFrac
                                exoPlayer.seekTo((initialFrac * durationMs).toLong())

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.pressed) {
                                        curX = change.position.x
                                        val frac = (curX / size.width).coerceIn(0f, 1f)
                                        scrubFraction = frac
                                        exoPlayer.seekTo((frac * durationMs).toLong())
                                        change.consume()
                                    }
                                } while (event.changes.any { it.pressed })

                                isScrubbing = false
                                scrubFraction = null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedTrackHeight)
                    ) {
                        val trackH = size.height
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.28f),
                            size = size,
                            cornerRadius = corner
                        )

                        val activeW = size.width * progress
                        drawRoundRect(
                            color = Color.White,
                            size = Size(activeW, trackH),
                            cornerRadius = corner
                        )

                        drawCircle(
                            color = Color.White,
                            radius = animatedThumbRadius.toPx(),
                            center = Offset(activeW, trackH / 2f)
                        )
                    }
                }

                // Таймер
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${formatTimeMs(currentPositionMs)} / ${formatTimeMs(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
actual fun MediaViewerSystemBars() {
    val view = LocalView.current

    val window = (
            view.parent as? DialogWindowProvider
            )?.window

    DisposableEffect(window) {
        val controller = window?.let {
            WindowCompat.getInsetsController(
                it,
                it.decorView
            )
        }

        val previousBehavior =
            controller?.systemBarsBehavior

        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller?.hide(
            WindowInsetsCompat.Type.systemBars()
        )

        onDispose {
            if (controller != null) {
                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )

                if (previousBehavior != null) {
                    controller.systemBarsBehavior =
                        previousBehavior
                }
            }
        }
    }
}

private fun formatTimeMs(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${if (minutes < 10) "0$minutes" else "$minutes"}:${if (seconds < 10) "0$seconds" else "$seconds"}"
}