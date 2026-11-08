package com.ghostgram.app.presentation.components.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ZoomableImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    onTransformGestureChanged: (Boolean) -> Unit = {}
) {
    // 💥 Используем Animatable: мгновенный snapTo при перетаскивании и плавный tween ТОЛЬКО при дабл-тапе!
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val dismissOffsetYAnim = remember { Animatable(0f) }

    val coroutineScope = rememberCoroutineScope()
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    // Сброс при смене картинки
    LaunchedEffect(model) {
        scaleAnim.snapTo(1f)
        offsetXAnim.snapTo(0f)
        offsetYAnim.snapTo(0f)
        dismissOffsetYAnim.snapTo(0f)
        onZoomChanged(false)
        onTransformGestureChanged(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 💥 1. ДВОЙНОЙ ТАП: ЗУМ 2.5X В ТОЧКУ НАЖАТИЯ / СБРОС В 1X
            .pointerInput(model) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapPosition ->
                        coroutineScope.launch {
                            if (scaleAnim.value > 1.05f) {
                                // Плавный сброс в 1x
                                onZoomChanged(false)
                                launch { scaleAnim.animateTo(1f, tween(200)) }
                                launch { offsetXAnim.animateTo(0f, tween(200)) }
                                launch { offsetYAnim.animateTo(0f, tween(200)) }
                            } else {
                                // Плавный зум в 2.5x
                                onZoomChanged(true)
                                val targetScale = 2.5f
                                val maxOffsetX = size.width * (targetScale - 1f) / 2f
                                val maxOffsetY = size.height * (targetScale - 1f) / 2f
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f

                                val targetX = ((centerX - tapPosition.x) * 1.5f).coerceIn(-maxOffsetX, maxOffsetX)
                                val targetY = ((centerY - tapPosition.y) * 1.5f).coerceIn(-maxOffsetY, maxOffsetY)

                                launch { scaleAnim.animateTo(targetScale, tween(200)) }
                                launch { offsetXAnim.animateTo(targetX, tween(200)) }
                                launch { offsetYAnim.animateTo(targetY, tween(200)) }
                            }
                        }
                    }
                )
            }
            // 💥 2. ПЕРЕМЕЩЕНИЕ: 1-В-1 ЗА ПАЛЬЦЕМ БЕЗ ТОРМОЗОВ!
            .pointerInput(model) {
                awaitEachGesture {
                    var isPinching = false
                    var isVerticalDrag = false
                    var totalDragX = 0f
                    var totalDragY = 0f

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointers = event.changes
                        val pressedPointers = pointers.filter { it.pressed }

                        // 💥 2 ПАЛЬЦА: ЗУМ
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
                            } else {
                                coroutineScope.launch {
                                    offsetXAnim.snapTo(0f)
                                    offsetYAnim.snapTo(0f)
                                }
                            }
                            pointers.forEach { it.consume() }
                        }
                        // 💥 1 ПАЛЕЦ
                        else if (pressedPointers.size == 1 && !isPinching) {
                            val change = pressedPointers.first()
                            val pan = change.positionChange()

                            if (scaleAnim.value > 1.05f) {
                                totalDragX += pan.x
                                totalDragY += pan.y

                                if (abs(totalDragX) > touchSlop || abs(totalDragY) > touchSlop) {
                                    val maxOffsetX = size.width * (scaleAnim.value - 1f) / 2f
                                    val maxOffsetY = size.height * (scaleAnim.value - 1f) / 2f
                                    // 💥 МГНОВЕННЫЙ SNAP_TO: картинка следует за пальцем со скоростью 120 FPS!
                                    coroutineScope.launch {
                                        offsetXAnim.snapTo((offsetXAnim.value + pan.x).coerceIn(-maxOffsetX, maxOffsetX))
                                        offsetYAnim.snapTo((offsetYAnim.value + pan.y).coerceIn(-maxOffsetY, maxOffsetY))
                                    }
                                    change.consume()
                                }
                            } else {
                                // Масштаб 1х: свайп вверх/вниз для закрытия
                                totalDragY += pan.y
                                totalDragX += pan.x

                                if (!isVerticalDrag) {
                                    if (abs(totalDragY) > touchSlop && abs(totalDragY) > abs(totalDragX) * 1.3f) {
                                        isVerticalDrag = true
                                    }
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

                    if (scaleAnim.value <= 1.05f) {
                        if (abs(dismissOffsetYAnim.value) > 160f) {
                            onDismiss()
                        } else {
                            coroutineScope.launch {
                                dismissOffsetYAnim.animateTo(0f, tween(150))
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                translationX = offsetXAnim.value
                translationY = offsetYAnim.value + dismissOffsetYAnim.value
                alpha = (1f - (abs(dismissOffsetYAnim.value) / 500f)).coerceIn(0.2f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        val finalModel = if (model.startsWith("/")) "file://$model" else model
        AsyncImage(
            model = finalModel,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}