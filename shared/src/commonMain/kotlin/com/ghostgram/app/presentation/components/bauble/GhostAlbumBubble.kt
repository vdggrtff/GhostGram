package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
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
import com.ghostgram.app.presentation.components.openVideoInSystemPlayer
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import entity.MessageMediaType

@Composable
fun GhostAlbumBubble(
    albumMessages: List<Message>,
    chatAvatarPath: String?,
    myAvatarPath: String?,
    chatTitle: String,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    replyMessage: Message? = null,
    onLongClick: () -> Unit,
    onMediaClick: (String) -> Unit
) {
    // Берем данные из первого сообщения (кто отправил, когда и тд)
    val baseMessage = albumMessages.last()
    val isOutgoing = baseMessage.isOutgoing

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (isFirstInGroup) 8.dp else 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOutgoing) {
            if (isLastInGroup) {
                if (chatAvatarPath != null) {
                    AsyncImage(
                        model = chatAvatarPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color(0xFFE58235)), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatTitle.take(1).uppercase().ifBlank { "💬" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Пустое место, чтобы пузыри не съезжали влево
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val cornerRadius = 16.dp
        val smallRadius = 4.dp

        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = if (isFirstInGroup) cornerRadius else smallRadius,
                bottomStart = cornerRadius,
                bottomEnd = if (isLastInGroup) cornerRadius else smallRadius
            )
        } else {
            RoundedCornerShape(
                topStart = if (isFirstInGroup) cornerRadius else smallRadius,
                topEnd = cornerRadius,
                bottomStart = if (isLastInGroup) cornerRadius else smallRadius,
                bottomEnd = cornerRadius
            )
        }
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 290.dp)
                .clip(bubbleShape)
                .combinedClickable( // 💥 ЛОВИМ ДОЛГИЙ ТАП
                    onClick = {},
                    onLongClick = onLongClick
                )
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
                if (baseMessage.replyToMessageId != 0L) {
                    // Если оригинал есть в памяти - берем его данные, иначе пишем заглушку
                    val replySenderName = replyMessage?.senderName ?: "Сообщение"
                    val replyText = replyMessage?.text?.ifBlank { "Медиафайл" } ?: "Загрузка..."

                    // Цвета зависят от того, исходящий ли это пузырь
                    val accentColor = if (isOutgoing) Color.White else GhostPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.1f)) // Легкий фон под цитатой
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Вертикальная полоска
                        Box(modifier = Modifier.width(3.dp).height(36.dp).background(accentColor))
                        Spacer(modifier = Modifier.width(8.dp))

                        // Текст цитаты
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = replySenderName, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = replyText,
                                color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else GhostTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // 💥 РИСУЕМ КАРТИНКИ СЕТКОЙ (По 2 в ряд)
                /*albumMessages.chunked(2).forEach { rowMessages ->
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
                }*/
                albumMessages.chunked(2).forEach { rowMessages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowMessages.forEach { msg ->

                            // 💥 1. ОПРЕДЕЛЯЕМ ТИП И ПУТИ
                            val isVideo = msg.mediaType == MessageMediaType.VIDEO || msg.mediaType == MessageMediaType.VIDEO_NOTE
                            val model = if (msg.photoPath?.startsWith("/") == true) "file://${msg.photoPath}" else msg.photoPath
                            val videoPath = msg.fileName

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f) // Квадратная ячейка альбома
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GhostSurfaceElevated) // Фон на случай загрузки
                                    // 💥 2. КЛИКАБЕЛЬНОСТЬ В ЗАВИСИМОСТИ ОТ ТИПА
                                    .clickable {
                                        if (isVideo && !msg.isSending && !videoPath.isNullOrBlank()) {
                                            // Открываем видео в плеере
                                            openVideoInSystemPlayer(videoPath)
                                        } else if (!isVideo && model != null) {
                                            // Открываем фото на фуллскрин
                                            onMediaClick(model)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Картинка или превью видео
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "Фото/Видео альбома",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (isVideo) {
                                    Text("🎬", fontSize = 24.sp) // Заглушка
                                }

                                // 💥 3. ЕСЛИ ЭТО ВИДЕО - РИСУЕМ UI ПЛЕЕРА!
                                if (isVideo) {
                                    // Затемнение
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                                    if (msg.isSending) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        // Кнопка PLAY
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("▶", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
                                        }

                                        // Длительность в левом верхнем углу ячейки
                                        Text(
                                            text = msg.fileExtraInfo ?: "0:00",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
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
            if (isLastInGroup) {
                if (myAvatarPath != null && !myAvatarPath.startsWith("INITIALS:")) {
                    AsyncImage(
                        model = myAvatarPath,
                        contentDescription = "Моя аватарка",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                } else {
                    val myName = myAvatarPath?.removePrefix("INITIALS:") ?: "Я"
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(GhostPrimary),
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
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
        }
    }
}