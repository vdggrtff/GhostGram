package com.ghostgram.app.utils.video

import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.ghostgram.app.ui.theme.GhostPrimary
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs

@Composable
actual fun InAppVideoPlayer(
    videoPath: String,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val cleanPath = videoPath.removePrefix("file://")

    var seekNoticeText by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember(cleanPath) {
        ExoPlayer.Builder(context).build().apply {
            val isUri =
                cleanPath.startsWith("content://") ||
                        cleanPath.startsWith("http://") ||
                        cleanPath.startsWith("https://")

            val file = File(cleanPath)

            if (isUri || file.exists()) {
                val uri = if (isUri) {
                    Uri.parse(cleanPath)
                } else {
                    Uri.fromFile(file)
                }

                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
        }
    }

    var isPlaying by remember(exoPlayer) {
        mutableStateOf(exoPlayer.isPlaying)
    }

    var currentPositionMs by remember(exoPlayer) {
        mutableStateOf(0L)
    }

    var durationMs by remember(exoPlayer) {
        mutableStateOf(0L)
    }

    // Соотношение сторон исходного видео.
    var videoAspectRatio by remember(cleanPath) {
        mutableStateOf(16f / 9f)
    }

    // Состояние зума видео.
    var videoScale by remember(cleanPath) {
        mutableStateOf(1f)
    }

    var videoOffset by remember(cleanPath) {
        mutableStateOf(Offset.Zero)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs =
                        exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (
                    videoSize.width > 0 &&
                    videoSize.height > 0
                ) {
                    val rawRatio =
                        videoSize.width *
                                videoSize.pixelWidthHeightRatio /
                                videoSize.height.toFloat()

                    videoAspectRatio =
                        if (
                            videoSize.unappliedRotationDegrees % 180 != 0
                        ) {
                            1f / rawRatio
                        } else {
                            rawRatio
                        }
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs =
                exoPlayer.currentPosition.coerceAtLeast(0L)

            durationMs =
                exoPlayer.duration.coerceAtLeast(0L)

            delay(200)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RectangleShape)
            .background(Color.Black)
    ) {
        // Масштабируем именно видео, а не контролы.
        // Исходное соотношение сторон сохраняется.
        val safeAspectRatio =
            videoAspectRatio.coerceIn(0.25f, 4f)

        val viewportAspectRatio =
            maxWidth.value /
                    maxHeight.value.coerceAtLeast(1f)

        val videoWidth: Dp
        val videoHeight: Dp

        if (safeAspectRatio >= viewportAspectRatio) {
            videoWidth = maxWidth
            videoHeight = maxWidth / safeAspectRatio
        } else {
            videoWidth = maxHeight * safeAspectRatio
            videoHeight = maxHeight
        }

        key(cleanPath) {
            AndroidView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        width = videoWidth,
                        height = videoHeight
                    )
                    .graphicsLayer {
                        scaleX = videoScale
                        scaleY = videoScale
                        translationX = videoOffset.x
                        translationY = videoOffset.y
                    },
                factory = { ctx ->
                    TextureView(ctx).also { textureView ->
                        exoPlayer.setVideoTextureView(textureView)
                    }
                }
            )
        }

        // Обработка касаний и жестов.
        // Этот слой располагается над видео, но под контролами.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cleanPath, controlsVisible) {
                    detectTapGestures(
                        onTap = {
                            onToggleControls()
                        },
                        onDoubleTap = { position ->
                            val screenWidth = size.width

                            when {
                                // 💥 1. ЛЕВАЯ ТРЕТЬ ЭКРАНА (< 33%): ПЕРЕМОТКА -10 СЕКУНД
                                position.x < screenWidth * 0.33f -> {
                                    val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    seekNoticeText = "-10 сек"
                                    if (!controlsVisible) onToggleControls()
                                }

                                // 💥 2. ПРАВАЯ ТРЕТЬ ЭКРАНА (> 66%): ПЕРЕМОТКА +10 СЕКУНД
                                position.x > screenWidth * 0.66f -> {
                                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                                    val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(duration)
                                    exoPlayer.seekTo(target)
                                    seekNoticeText = "+10 сек"
                                    if (!controlsVisible) onToggleControls()
                                }

                                // 💥 3. ЦЕНТР ЭКРАНА (33% - 66%): ЗУМ 2.5X И СБРОС В 1X!
                                else -> {
                                    if (videoScale > 1.05f) {
                                        videoScale = 1f
                                        videoOffset = Offset.Zero
                                    } else {
                                        videoScale = 2.5f
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(cleanPath) {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false
                        )

                        var totalX = 0f
                        var totalY = 0f
                        var dragAxis = 0
                        var hadMultipleTouches = false

                        do {
                            val event = awaitPointerEvent(
                                PointerEventPass.Main
                            )

                            val pressedPointers =
                                event.changes.filter { it.pressed }

                            when {
                                // Два пальца: изменение масштаба.
                                pressedPointers.size >= 2 -> {
                                    hadMultipleTouches = true

                                    val zoomChange =
                                        event.calculateZoom()

                                    val panChange =
                                        event.calculatePan()

                                    val newScale =
                                        (
                                                videoScale * zoomChange
                                                ).coerceIn(1f, 4f)

                                    if (newScale <= 1.02f) {
                                        videoScale = 1f
                                        videoOffset = Offset.Zero
                                    } else {
                                        videoScale = newScale

                                        val maxOffsetX =
                                            size.width *
                                                    (newScale - 1f) / 2f

                                        val maxOffsetY =
                                            size.height *
                                                    (newScale - 1f) / 2f

                                        videoOffset = Offset(
                                            x = (
                                                    videoOffset.x +
                                                            panChange.x
                                                    ).coerceIn(
                                                    -maxOffsetX,
                                                    maxOffsetX
                                                ),
                                            y = (
                                                    videoOffset.y +
                                                            panChange.y
                                                    ).coerceIn(
                                                    -maxOffsetY,
                                                    maxOffsetY
                                                )
                                        )
                                    }

                                    event.changes.forEach {
                                        it.consume()
                                    }
                                }

                                // Один палец: панорамирование
                                // увеличенного видео или закрытие.
                                pressedPointers.size == 1 &&
                                        !hadMultipleTouches -> {

                                    val change =
                                        pressedPointers.first()

                                    val delta =
                                        change.position -
                                                change.previousPosition

                                    if (videoScale > 1.05f) {
                                        val maxOffsetX =
                                            size.width *
                                                    (videoScale - 1f) / 2f

                                        val maxOffsetY =
                                            size.height *
                                                    (videoScale - 1f) / 2f

                                        videoOffset = Offset(
                                            x = (
                                                    videoOffset.x + delta.x
                                                    ).coerceIn(
                                                    -maxOffsetX,
                                                    maxOffsetX
                                                ),
                                            y = (
                                                    videoOffset.y + delta.y
                                                    ).coerceIn(
                                                    -maxOffsetY,
                                                    maxOffsetY
                                                )
                                        )

                                        if (
                                            delta.x != 0f ||
                                            delta.y != 0f
                                        ) {
                                            change.consume()
                                        }
                                    } else {
                                        totalX += delta.x
                                        totalY += delta.y

                                        val touchSlop =
                                            viewConfiguration.touchSlop

                                        if (dragAxis == 0) {
                                            if (
                                                abs(totalY) > touchSlop &&
                                                abs(totalY) >
                                                abs(totalX) * 1.2f
                                            ) {
                                                dragAxis = 2
                                            } else if (
                                                abs(totalX) > touchSlop &&
                                                abs(totalX) >
                                                abs(totalY)
                                            ) {
                                                dragAxis = 1
                                            }
                                        }

                                        // Горизонтальный жест оставляем
                                        // HorizontalPager.
                                        // Вертикальный используем
                                        // для закрытия просмотрщика.
                                        if (dragAxis == 2) {
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        } while (
                            event.changes.any { it.pressed }
                        )

                        val dismissThreshold =
                            size.height * 0.18f

                        if (
                            !hadMultipleTouches &&
                            videoScale <= 1.05f &&
                            dragAxis == 2 &&
                            abs(totalY) > dismissThreshold
                        ) {
                            onDismiss()
                        }
                    }
                }
        )

        if (seekNoticeText != null) {
            LaunchedEffect(seekNoticeText) {
                kotlinx.coroutines.delay(800)
                seekNoticeText = null // Сама исчезает через 0.8 сек
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = seekNoticeText!!,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Центральная кнопка Play / Pause.
        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Color.Black.copy(alpha = 0.65f)
                    )
                    .clickable {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (
                                exoPlayer.playbackState ==
                                Player.STATE_ENDED
                            ) {
                                exoPlayer.seekTo(0L)
                            }

                            exoPlayer.play()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Таймлайн.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
            ) {
                Slider(
                    value = if (durationMs > 0L) {
                        (
                                currentPositionMs.toFloat() /
                                        durationMs.toFloat()
                                ).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    onValueChange = { fraction ->
                        val position =
                            (fraction * durationMs).toLong()

                        currentPositionMs = position
                        exoPlayer.seekTo(position)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = GhostPrimary,
                        activeTrackColor = GhostPrimary,
                        inactiveTrackColor =
                            Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMs(currentPositionMs),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = formatTimeMs(durationMs),
                        color = Color.White.copy(alpha = 0.8f),
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

private fun formatTimeMs(ms: Long): String {
    val totalSeconds =
        (ms.coerceAtLeast(0L) / 1000).toInt()

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "${minutes.toString().padStart(2, '0')}:" +
            seconds.toString().padStart(2, '0')
}