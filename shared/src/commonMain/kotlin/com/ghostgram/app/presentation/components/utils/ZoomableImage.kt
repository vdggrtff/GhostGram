package com.ghostgram.app.presentation.components.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlin.math.abs

@Composable
fun ZoomableImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dismissOffsetY by remember { mutableStateOf(0f) }

    val animatedDismissOffsetY by animateFloatAsState(targetValue = dismissOffsetY, label = "dismiss")

    LaunchedEffect(model) {
        scale = 1f
        offset = Offset.Zero
        dismissOffsetY = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 💥 1. ДВОЙНОЙ КЛИК: ЗУМ 2.5X И ЧЕТКИЙ СБРОС НАЗАД В 1X!
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            // Анзум назад!
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            // Зум вперед!
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            // 💥 2. ТАЧ: 2 ПАЛЬЦА (ЗУМ), 1 ПАЛЕЦ (ПЕРЕМЕЩЕНИЕ ИЛИ СВАЙП ВЫХОДА)
            .pointerInput(Unit) {
                awaitEachGesture {
                    var isPinching = false
                    var isVerticalDrag = false

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointers = event.changes

                        // ДВА ПАЛЬЦА: ЗУМ
                        if (pointers.size >= 2) {
                            isPinching = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1.05f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                            pointers.forEach { it.consume() }
                        }
                        // ОДИН ПАЛЕЦ
                        else if (pointers.size == 1 && !isPinching) {
                            val change = pointers[0]
                            val pan = change.positionChange()

                            if (scale > 1.05f) {
                                // 💥 ГЛАВНЫЙ ФИКС: поглощаем касание ТОЛЬКО если палец реально сдвинулся!
                                // Если палец нажат на месте (двойной клик) — не поглощаем, даем сработать doubleTap!
                                if (abs(pan.x) > 0.5f || abs(pan.y) > 0.5f) {
                                    offset += pan
                                    change.consume()
                                }
                            } else {
                                // Масштаб 1х: проверяем свайп вверх/вниз для закрытия
                                if (!isVerticalDrag) {
                                    if (abs(pan.y) > abs(pan.x) && abs(pan.y) > 2f) {
                                        isVerticalDrag = true
                                    }
                                }

                                if (isVerticalDrag) {
                                    dismissOffsetY += pan.y
                                    change.consume()
                                }
                                // Горизонтальный свайп не поглощаем — листает фотки в HorizontalPager!
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (scale <= 1.05f) {
                        if (abs(dismissOffsetY) > 140f) {
                            onDismiss() // Смахнули фото вверх или вниз — выход!
                        } else {
                            dismissOffsetY = 0f
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y + animatedDismissOffsetY
                alpha = (1f - (abs(animatedDismissOffsetY) / 500f)).coerceIn(0.2f, 1f)
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