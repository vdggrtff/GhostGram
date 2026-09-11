package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import kotlin.collections.chunked

@Composable
fun GhostAlbumBubble(
    albumMessages: List<Message>,
    chatAvatarPath: String?,
    myAvatarPath: String?,
    chatTitle: String,
    onMediaClick: (String) -> Unit
) {
    // Берем данные из первого сообщения (кто отправил, когда и тд)
    val baseMessage = albumMessages.last()
    val isOutgoing = baseMessage.isOutgoing

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // ... (Аватарка собеседника слева - скопируй из GhostMessageBubble) ...

        val bubbleShape = if (isOutgoing) RoundedCornerShape(
            16.dp,
            16.dp,
            16.dp,
            4.dp
        ) else RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 290.dp)
                .clip(bubbleShape)
                .background(
                    if (isOutgoing) Brush.linearGradient(
                        listOf(
                            GhostPrimary,
                            GhostSecondary
                        )
                    ) else Brush.linearGradient(listOf(GhostCard, GhostCard))
                )
                .padding(6.dp) // Чуть меньше паддинги для альбома
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                // 💥 РИСУЕМ КАРТИНКИ СЕТКОЙ (По 2 в ряд)
                albumMessages.chunked(2).forEach { rowMessages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowMessages.forEach { msg ->
                            val model =
                                if (msg.photoPath?.startsWith("/") == true) "file://${msg.photoPath}" else msg.photoPath
                            AsyncImage(
                                model = model,
                                contentDescription = "Фото альбома",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f) // Квадратные картинки
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { if (model != null) onMediaClick(model) }
                            )
                        }
                    }
                }

                // Ищем подпись к альбому (обычно она у одной из фоток)
                val caption = albumMessages.map { it.text }.firstOrNull { it.isNotBlank() }
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption, color = Color.White, fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Время и галочки (берем у последнего сообщения)
                val timeString = TimeFormatter.formatTime(baseMessage.date)
                Row(
                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (baseMessage.isRead) androidx.compose.material.icons.Icons.Default.DoneAll else androidx.compose.material.icons.Icons.Default.Done,
                            contentDescription = "Статус",
                            tint = if (baseMessage.isRead) Color(0xFF4FC3F7) else Color.White.copy(
                                alpha = 0.6f
                            ),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        if (isOutgoing) {
            Spacer(modifier = Modifier.width(8.dp))
            if (myAvatarPath != null && !myAvatarPath.startsWith("INITIALS:")) {
                AsyncImage(
                    model = myAvatarPath,
                    contentDescription = "Моя аватарка",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            } else {
                // Красивая заглушка с инициалами
                val myName = myAvatarPath?.removePrefix("INITIALS:") ?: "Я"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GhostPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = myName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}