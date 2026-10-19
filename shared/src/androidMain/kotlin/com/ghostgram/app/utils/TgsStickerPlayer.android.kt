package com.ghostgram.app.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ghostgram.app.presentation.components.TgsFrameBaker
import com.ghostgram.app.ui.theme.GhostPrimary
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

private const val TGS_WIDTH_PX = 260
private const val TGS_HEIGHT_PX = 260
private const val TGS_TARGET_FPS = 30

@Composable
actual fun TgsStickerPlayer(
    tgsJson: String,
    modifier: Modifier
) {
    val cacheKey = remember(tgsJson) {
        "tgs:${tgsJson.hashCode()}:" +
                "${TGS_WIDTH_PX}x$TGS_HEIGHT_PX:" +
                "${TGS_TARGET_FPS}fps"
    }

    var bakedAnimation by remember(cacheKey) {
        mutableStateOf(
            TgsFrameBaker.getCachedFrames(cacheKey)
        )
    }

    var bakeFinished by remember(cacheKey) {
        mutableStateOf(bakedAnimation != null)
    }

    LaunchedEffect(cacheKey, tgsJson) {
        if (bakedAnimation == null) {
            bakedAnimation = TgsFrameBaker.bake(
                tgsJson = tgsJson,
                cacheKey = cacheKey,
                widthPx = TGS_WIDTH_PX,
                heightPx = TGS_HEIGHT_PX,
                targetFps = TGS_TARGET_FPS
            )
        }

        bakeFinished = true
    }

    val animation = bakedAnimation

    if (animation == null) {
        if (!bakeFinished) {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = GhostPrimary,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Spacer(modifier = modifier)
        }

        return
    }

    val frames = animation.frames
    val durationMs = animation.durationMs

    if (frames.isEmpty()) {
        Spacer(modifier = modifier)
        return
    }

    if (frames.size == 1 || durationMs <= 0L) {
        Canvas(modifier = modifier) {
            val frame = frames.first()

            val scale = minOf(
                size.width / frame.width.toFloat(),
                size.height / frame.height.toFloat()
            )

            val width = (frame.width * scale).roundToInt()
            val height = (frame.height * scale).roundToInt()

            if (width > 0 && height > 0) {
                drawImage(
                    image = frame,
                    dstOffset = IntOffset(
                        ((size.width - width) / 2f).roundToInt(),
                        ((size.height - height) / 2f).roundToInt()
                    ),
                    dstSize = IntSize(width, height),
                    filterQuality = FilterQuality.Medium
                )
            }
        }

        return
    }

    var frameIndex by remember(cacheKey) {
        mutableStateOf(0)
    }

    LaunchedEffect(cacheKey, frames.size, durationMs) {
        val frameDurationNanos =
            durationMs * 1_000_000L / frames.size

        var previousFrameTime = 0L
        var elapsedNanos = 0L

        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (previousFrameTime == 0L) {
                    previousFrameTime = frameTimeNanos
                } else {
                    elapsedNanos +=
                        frameTimeNanos - previousFrameTime

                    previousFrameTime = frameTimeNanos

                    if (elapsedNanos >= frameDurationNanos) {
                        val framesToAdvance =
                            (elapsedNanos / frameDurationNanos)
                                .toInt()

                        elapsedNanos %= frameDurationNanos

                        frameIndex =
                            (frameIndex + framesToAdvance) % frames.size
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val frame = frames[frameIndex]

        val scale = minOf(
            size.width / frame.width.toFloat(),
            size.height / frame.height.toFloat()
        )

        val width = (frame.width * scale).roundToInt()
        val height = (frame.height * scale).roundToInt()

        if (width > 0 && height > 0) {
            drawImage(
                image = frame,
                dstOffset = IntOffset(
                    ((size.width - width) / 2f).roundToInt(),
                    ((size.height - height) / 2f).roundToInt()
                ),
                dstSize = IntSize(width, height),
                filterQuality = FilterQuality.Medium
            )
        }
    }
}