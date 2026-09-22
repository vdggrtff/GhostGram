package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.utils.openVideoInSystemPlayer
import com.ghostgram.app.utils.TimeFormatter
import entity.Message

@Composable
fun VideoMessageContent(
    message: Message,
    bubbleShape: RoundedCornerShape,
    hasText: Boolean,
    isOutgoing: Boolean
) {
    val thumbModel = message.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }
    // 💥 ПУТЬ К САМОМУ ВИДЕО (Который мы сохранили в fileName)
    val videoPath = message.fileName

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(180.dp) // Фиксированная высота карточки
            .clip(bubbleShape)
            .background(Color(0xFF1E2330)) // Темный стильный фон, если превью еще грузится
            .clickable {
                if (!message.isSending && !videoPath.isNullOrBlank()) {
                    // Запускаем в системном плеере (VLC, и т.д.)
                    openVideoInSystemPlayer(videoPath)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Превью (если есть)
        if (thumbModel != null) {
            AsyncImage(model = thumbModel, contentDescription = "Превью", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        } else {
            // Если превью нет (особенность десктопа) - просто пишем, что это видео
            Text("🎬", fontSize = 40.sp)
        }

        // Затемнение
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

        // 💥 КНОПКА PLAY ИЛИ СПИННЕР ПО ЦЕНТРУ
        if (message.isSending) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(44.dp), strokeWidth = 3.dp)
        } else {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(start = 2.dp))
            }
        }

        // 💥 ХРОНОМЕТРАЖ В ВЕРХНЕМ ЛЕВОМ УГЛУ (КАК НА СКРИНЕ!)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = message.fileExtraInfo?.ifBlank { "00:00" } ?: "00:00",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 💥 ВРЕМЯ И ГАЛОЧКА В ПРАВОМ НИЖНЕМ УГЛУ (КАК НА СКРИНЕ!)
        if (!hasText) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeFormatter.formatTime(message.date),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isSending) Icons.Default.Schedule else if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}