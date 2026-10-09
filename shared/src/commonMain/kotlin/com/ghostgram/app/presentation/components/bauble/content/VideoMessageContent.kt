package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    bubbleShape: Shape,
    hasText: Boolean,
    isOutgoing: Boolean,
    onMediaClick: (String) -> Unit = {}
) {
    val thumbModel = message.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }
    val videoPath = message.fileName

    Box(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2330))
            .clickable {
                if (!message.isSending && !videoPath.isNullOrBlank()) {
                    openVideoInSystemPlayer(videoPath)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Превьюшка видео с адаптивной высотой
        if (thumbModel != null) {
            AsyncImage(
                model = thumbModel,
                contentDescription = "Превью видео",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 340.dp)
            )
            // Легкое киношное затемнение
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
        } else {
            // Если превью еще генерируется
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF1E2330)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎬", fontSize = 32.sp)
            }
        }

        // 💥 ПО ЦЕНТРУ: КРУТИЛКА ЗАГРУЗКИ ИЛИ КНОПКА PLAY
        if (message.isSending || (videoPath.isNullOrBlank() && thumbModel == null)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (message.isSending) "Отправка..." else "Загрузка видео...",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Кнопка PLAY
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).padding(start = 2.dp)
                )
            }

            // Хронометраж видео в левом верхнем углу (как в Telegram!)
            val durationText = message.fileExtraInfo
            if (!durationText.isNullOrBlank() && durationText != "0:00") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
