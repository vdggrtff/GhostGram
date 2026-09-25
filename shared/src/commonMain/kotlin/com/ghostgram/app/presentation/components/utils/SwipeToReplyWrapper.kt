package com.ghostgram.app.presentation.components.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ghostgram.app.ui.theme.GhostPrimary
import kotlin.math.roundToInt

@Composable
fun SwipeToReplyWrapper(
    onSwipe: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe")

    // Порог, после которого срабатывает ответ
    val triggerThreshold = -150f

    LaunchedEffect(offsetX) {
        if (offsetX <= triggerThreshold) {
            onSwipe() // Срабатывает коллбэк!
            offsetX = 0f // Возвращаем пузырь на место
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { offsetX = 0f }, // Бросили - вернулось обратно
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetX + dragAmount
                        // Разрешаем тянуть только влево (до -200px)
                        if (newOffset < 0f && newOffset > -200f) {
                            offsetX = newOffset
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        // ИКОНКА ОТВЕТА (Появляется из-под сообщения)
        if (animatedOffsetX < -20f) {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GhostPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Reply, contentDescription = "Reply", tint = GhostPrimary, modifier = Modifier.size(18.dp))
            }
        }

        // САМ ПУЗЫРЬ СООБЩЕНИЯ (Двигается за пальцем)
        Box(
            modifier = Modifier.offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        ) {
            content()
        }
    }
}