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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.ghostgram.app.utils.openVideoInSystemPlayer
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.ui.theme.TelegramColors
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import entity.MessageMediaType

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GhostAlbumBubble(
    albumMessages: List<Message>,
    chatAvatarPath: String?,
    myAvatarPath: String?,
    chatTitle: String,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    isGroup: Boolean = false, // Флаг группы
    replyMessage: Message? = null,
    onLongClick: () -> Unit,
    onMediaClick: (String) -> Unit
) {
    val baseMessage = albumMessages.last()
    val isOutgoing = baseMessage.isOutgoing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) 8.dp else 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {

        // 1. АВАТАРКА АВТОРА СЛЕВА (РИСУЕТСЯ ТОЛЬКО В ГРУППАХ!)
        if (isGroup && !isOutgoing) {
            if (isLastInGroup) {
                val avatarToShow = baseMessage.senderAvatarPath
                val authorName = baseMessage.senderName.ifBlank { "Участник" }
                val authorColor = TelegramColors.getColorForUser(baseMessage.senderId)

                key(avatarToShow, authorName) {
                    if (!avatarToShow.isNullOrBlank() && !avatarToShow.startsWith("INITIALS:")) {
                        val model = if (avatarToShow.startsWith("/")) "file://$avatarToShow" else avatarToShow
                        AsyncImage(
                            model = model,
                            contentDescription = "Аватарка автора",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                        )
                    } else {
                        // Цветной кружок с буквой автора
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(authorColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Хвостики углов
        val cornerRadius = 16.dp
        val sharpCorner = 3.dp

        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = if (isFirstInGroup) cornerRadius else sharpCorner,
                bottomStart = cornerRadius,
                bottomEnd = if (isLastInGroup) sharpCorner else cornerRadius
            )
        } else {
            RoundedCornerShape(
                topStart = if (isFirstInGroup) cornerRadius else sharpCorner,
                topEnd = cornerRadius,
                bottomStart = if (isLastInGroup) sharpCorner else cornerRadius,
                bottomEnd = cornerRadius
            )
        }

        // ТОЧНЫЕ ЦВЕТА TELEGRAM
        val outgoingBackground = if (baseMessage.fileExtraInfo == "ENCRYPTED") Color(0xFF1A4A38) else Color(0xFF5274E8)
        val incomingBackground = Color(0xFF18222D)

        Box(
            modifier = Modifier
                .widthIn(min = 180.dp, max = 290.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .background(if (isOutgoing) outgoingBackground else incomingBackground)
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                // 2. ЦВЕТНОЕ ИМЯ АВТОРА В ГРУППЕ (НАД АЛЬБОМОМ)
                if (isGroup && !isOutgoing && isFirstInGroup) {
                    val authorColor = TelegramColors.getColorForUser(baseMessage.senderId)
                    Text(
                        text = baseMessage.senderName.ifBlank { "Участник" },
                        color = authorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Цитата (Reply), если есть
                if (baseMessage.replyToMessageId != 0L) {
                    val replySenderName = replyMessage?.senderName ?: "Сообщение"
                    val replyText = replyMessage?.text?.ifBlank { "Медиафайл" } ?: "Загрузка..."
                    val accentColor = if (isOutgoing) Color.White else GhostPrimary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(34.dp).background(accentColor))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = replySenderName, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = replyText,
                                color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else GhostTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // СЕТКА КАРТИНОК И ВИДЕО (2 в ряд)
                albumMessages.chunked(2).forEach { rowMessages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowMessages.forEach { msg ->
                            val isVideo = msg.mediaType == MessageMediaType.VIDEO || msg.mediaType == MessageMediaType.VIDEO_NOTE
                            val model = if (msg.photoPath?.startsWith("/") == true) "file://${msg.photoPath}" else msg.photoPath
                            val videoPath = msg.fileName

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GhostSurfaceElevated)
                                    .clickable {
                                        if (isVideo && !msg.isSending && !videoPath.isNullOrBlank()) {
                                            openVideoInSystemPlayer(videoPath)
                                        } else if (!isVideo && model != null) {
                                            onMediaClick(model)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "Медиа альбома",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (isVideo) {
                                    Text("🎬", fontSize = 24.sp)
                                }

                                if (isVideo) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                                    if (msg.isSending) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("▶", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
                                        }

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

                // Подпись альбома
                val caption = albumMessages.map { it.text }.firstOrNull { it.isNotBlank() }
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption, color = Color.White, fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Время и галочки
                val timeString = TimeFormatter.formatTime(baseMessage.date)
                Row(
                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = timeString, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (baseMessage.isSending) Icons.Default.Schedule else if (baseMessage.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = if (baseMessage.isRead) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}