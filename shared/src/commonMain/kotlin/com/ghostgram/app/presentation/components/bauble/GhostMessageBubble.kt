package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ghostgram.app.presentation.components.bauble.content.DocumentMessageContent
import com.ghostgram.app.presentation.components.bauble.content.ImageMessageContent
import com.ghostgram.app.presentation.components.bauble.content.StickerMessageContent
import com.ghostgram.app.presentation.components.bauble.content.VideoMessageContent
import com.ghostgram.app.presentation.components.bauble.content.VoiceMessageContent
import com.ghostgram.app.presentation.components.bauble.layout.ChatMessageLayout
import com.ghostgram.app.presentation.components.bauble.layout.MessageTimeAndStatus
import com.ghostgram.app.presentation.components.utils.ReplyToMessage
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.TelegramColors
import entity.Message
import entity.MessageMediaType

@Composable
fun GhostMessageBubble(
    message: Message,
    isGroup: Boolean = false,
    isFirstInGroup: Boolean = true, // Новые параметры
    isLastInGroup: Boolean = true,
    replyMessage: Message? = null,
    onLongClick: () -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.isOutgoing
    val isVideo = message.mediaType == MessageMediaType.VIDEO || message.mediaType == MessageMediaType.VIDEO_NOTE
    val isSticker = message.mediaType == MessageMediaType.STICKER
    val hasText = message.text.isNotBlank()

    // Системное сообщение (например, E2EE handshake)
    if (message.fileExtraInfo == "SYSTEM") {
        SystemMessageBadge(message.text)
        return
    }

    // Внешний ряд, который держит Аватарки и Пузырь
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) 8.dp else 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Аватарки прижаты к низу
    ) {
        if (isGroup && !isOutgoing) {
            if (isLastInGroup) {
                SenderAvatar(message)
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val bubbleShape = rememberBubbleShape(isOutgoing, isFirstInGroup, isLastInGroup)
        val backgroundColor = rememberBubbleColor(message, isOutgoing)

        Box(
            modifier = Modifier
                .widthIn(min = if (isVideo) 240.dp else 0.dp, max = 290.dp) // Ограничиваем только максимум, чтобы он сжимался под текст
                .clip(bubbleShape)
                .combinedClickable( // ЛОВИМ ДОЛГИЙ ТАП
                    onClick = {},
                    onLongClick = onLongClick
                )
                .then(
                    when {
                        isSticker || (isVideo && !hasText) -> Modifier.background(Color.Transparent)
                        else -> Modifier.background(backgroundColor)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier.wrapContentWidth(), // Пузырь плотно облегает контент
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGroup && !isOutgoing && isFirstInGroup) {
                    val authorColor = TelegramColors.getColorForUser(message.senderId)
                    Text(
                        text = message.senderName.ifBlank { "Участник" },
                        color = authorColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (message.replyToMessageId != 0L) {
                    ReplyToMessage(
                        replyMessage = replyMessage,
                        isOutgoing = isOutgoing
                    )
                }

                when (message.mediaType) {
                    MessageMediaType.PHOTO -> {
                        if (message.photoPath != null) {
                            ImageMessageContent(
                                message = message,
                                onMediaClick = onMediaClick
                            )
                        }
                    }

                    MessageMediaType.VIDEO, MessageMediaType.VIDEO_NOTE -> {
                        VideoMessageContent(
                            message = message,
                            bubbleShape = bubbleShape,
                            hasText = hasText,
                            isOutgoing = isOutgoing
                        )
                    }

                    MessageMediaType.DOCUMENT -> {
                        DocumentMessageContent(
                            message = message
                        )
                    }

                    MessageMediaType.STICKER -> {
                        if (message.photoPath != null) {
                            StickerMessageContent(
                                message = message,
                            )
                        }
                    }

                    MessageMediaType.VOICE -> {
                        VoiceMessageContent(message = message)
                    }

                    MessageMediaType.TEXT -> { /* Обычный текст, ничего не делаем */
                    }
                }
                // 🗑 Метка Anti-Revoke
                if (message.isDeletedLocally) {
                    Text(
                        text = "🗑️ Удалено отправителем",
                        color = GhostAccentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasText) {
                    val isEncrypted = message.fileExtraInfo == "ENCRYPTED"
                    ChatMessageLayout(
                        text = {
                            Text(
                                text = if (isEncrypted) "🔒 ${message.text}" else message.text,
                                color = if (isEncrypted) GhostSecureGreen else Color.White,
                                fontSize = 16.sp,
                                lineHeight = 21.sp
                            )
                        },
                        time = { MessageTimeAndStatus(message, Color.White, Color.White) }
                    )
                }
            }
            if (isSticker || (message.photoPath != null && message.text.isBlank())) {
                MessageTimeAndStatus(
                    message = message,
                    textColor = Color.White,
                    iconColor = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SenderAvatar(message: Message) {
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
                Text(text = authorName.take(1).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SystemMessageBadge(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun rememberBubbleShape(isOutgoing: Boolean, isFirstInGroup: Boolean, isLastInGroup: Boolean): RoundedCornerShape {
    val corner = 10.dp
    val sharp = 2.dp
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

private fun rememberBubbleColor(message: Message, isOutgoing: Boolean): Color {
    return when {
        message.fileExtraInfo == "ENCRYPTED" -> Color(0xFF1A4A38)
        isOutgoing -> Color(0xFF5274E8)
        else -> Color(0xFF18222D)
    }
}
