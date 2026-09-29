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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.bauble.layout.MessageTimeAndStatus
import com.ghostgram.app.presentation.components.utils.ReplyToMessage
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.TelegramColors
import com.ghostgram.app.utils.openVideoInSystemPlayer
import entity.Message
import entity.MessageMediaType

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GhostAlbumBubble(
    albumMessages: List<Message>,
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
                AlbumSenderAvatar(baseMessage)
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val bubbleShape = rememberAlbumShape(isOutgoing, isFirstInGroup, isLastInGroup)
        val backgroundColor = rememberAlbumColor(baseMessage, isOutgoing)

        Box(
            modifier = Modifier
                .widthIn(min = 180.dp, max = 290.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .background(backgroundColor)
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
                    ReplyToMessage(replyMessage = replyMessage, isOutgoing = isOutgoing)
                }

                // СЕТКА КАРТИНОК И ВИДЕО (2 в ряд)
                albumMessages.chunked(2).forEach { rowMessages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowMessages.forEach { msg ->
                            AlbumMediaItem(
                                message = msg,
                                modifier = Modifier.weight(1f),
                                onMediaClick = onMediaClick
                            )
                        }
                    }
                }

                // Подпись альбома
                val caption = albumMessages.map { it.text }.firstOrNull { it.isNotBlank() }
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Время и галочки
                MessageTimeAndStatus(
                    message = baseMessage,
                    textColor = Color.White,
                    iconColor = Color.White,
                    modifier = Modifier.align(Alignment.End).padding(end = 4.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumMediaItem(
    message: Message,
    modifier: Modifier = Modifier,
    onMediaClick: (String) -> Unit
) {
    val isVideo = message.mediaType == MessageMediaType.VIDEO || message.mediaType == MessageMediaType.VIDEO_NOTE
    val model = if (message.photoPath?.startsWith("/") == true) "file://${message.photoPath}" else message.photoPath
    val videoPath = message.fileName

    Box(
        modifier = modifier
            .aspectRatio(1f) // Квадратная ячейка
            .clip(RoundedCornerShape(8.dp))
            .background(GhostSurfaceElevated)
            .clickable {
                if (isVideo && !message.isSending && !videoPath.isNullOrBlank()) {
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
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isVideo) {
            Text("🎬", fontSize = 24.sp)
        }

        // Оверлей для видео
        if (isVideo) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            if (message.isSending) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
                }

                Text(
                    text = message.fileExtraInfo ?: "0:00",
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

@Composable
private fun AlbumSenderAvatar(message: Message) {
    val avatarToShow = message.senderAvatarPath
    val authorName = message.senderName.ifBlank { "Участник" }
    val authorColor = TelegramColors.getColorForUser(message.senderId)

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
}

private fun rememberAlbumShape(isOutgoing: Boolean, isFirstInGroup: Boolean, isLastInGroup: Boolean): RoundedCornerShape {
    val corner = 16.dp
    val sharp = 3.dp
    return if (isOutgoing) {
        RoundedCornerShape(
            topStart = corner,
            topEnd = if (isFirstInGroup) corner else sharp,
            bottomStart = corner,
            bottomEnd = if (isLastInGroup) sharp else corner
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) corner else sharp,
            topEnd = corner,
            bottomStart = if (isLastInGroup) sharp else corner,
            bottomEnd = corner
        )
    }
}

private fun rememberAlbumColor(message: Message, isOutgoing: Boolean): Color {
    return when {
        message.fileExtraInfo == "ENCRYPTED" -> Color(0xFF1A4A38)
        isOutgoing -> Color(0xFF5274E8)
        else -> Color(0xFF18222D)
    }
}