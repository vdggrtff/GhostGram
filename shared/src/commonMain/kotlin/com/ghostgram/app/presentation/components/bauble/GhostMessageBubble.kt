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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.bauble.content.DocumentMessageContent
import com.ghostgram.app.presentation.components.bauble.content.ImageMessageContent
import com.ghostgram.app.presentation.components.bauble.content.StickerMessageContent
import com.ghostgram.app.presentation.components.bauble.content.VideoMessageContent
import com.ghostgram.app.presentation.components.bauble.content.VoiceMessageContent
import com.ghostgram.app.presentation.components.utils.ReplyToMessage
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import entity.MessageMediaType

@Composable
fun GhostMessageBubble(
    message: Message,
    chatAvatarPath: String?,
    myAvatarPath: String?, // Вернули твою аватарку!
    chatTitle: String,
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

    if (message.fileExtraInfo == "SYSTEM") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        return // Выходим из функции, чтобы не рисовать синий пузырь!
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
                val avatarToShow = message.senderAvatarPath
                val authorName = message.senderName.ifBlank { "Участник" }
                val authorColor = com.ghostgram.app.ui.theme.TelegramColors.getColorForUser(message.senderId)

                androidx.compose.runtime.key(avatarToShow, authorName) {
                    if (!avatarToShow.isNullOrBlank() && !avatarToShow.startsWith("INITIALS:")) {
                        val model = if (avatarToShow.startsWith("/")) "file://$avatarToShow" else avatarToShow
                        AsyncImage(
                            model = model,
                            contentDescription = "Аватарка автора",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp).clip(CircleShape)
                        )
                    } else {
                        // Кружок цвета автора с его первой буквой имени
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
                // Отступ 34dp, чтобы пачка сообщений одного человека стояла ровно
                Spacer(modifier = Modifier.width(34.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val cornerRadius = 10.dp
        val sharpCorner = 2.dp // Острый хвостик у основания

        val bubbleShape = if (isOutgoing) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = if (isFirstInGroup) cornerRadius else sharpCorner,
                bottomStart = cornerRadius,
                bottomEnd = if (isLastInGroup) sharpCorner else cornerRadius // Острый угол к автору
            )
        } else {
            RoundedCornerShape(
                topStart = if (isFirstInGroup) cornerRadius else sharpCorner,
                topEnd = cornerRadius,
                bottomStart = if (isLastInGroup) sharpCorner else cornerRadius,
                bottomEnd = cornerRadius
            )
        }

        val outgoingBackground = if (message.fileExtraInfo == "ENCRYPTED") {
            Color(0xFF1A4A38) // Изумруд для шифровки
        } else {
            Color(0xFF5274E8) // Фирменный королевский синий Telegram
        }
        val incomingBackground = Color(0xFF18222D)

        Box(
            modifier = Modifier
                .widthIn(min = if (isVideo) 240.dp else 0.dp, max = 290.dp) // Ограничиваем только максимум, чтобы он сжимался под текст
                .clip(bubbleShape)
                .combinedClickable( // ЛОВИМ ДОЛГИЙ ТАП
                    onClick = {},
                    onLongClick = onLongClick
                )
                .then(
                    if (isSticker) {
                        Modifier.background(Color.Transparent) // Стикеры без фона!
                    }else if (isVideo && !hasText) {
                        Modifier.background(Color.Transparent)
                    }
                    else if (isOutgoing) {
                        Modifier.background(
                            outgoingBackground
                        )
                    } else {
                        Modifier.background(incomingBackground)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Column(
                modifier = Modifier.wrapContentWidth(), // Пузырь плотно облегает контент
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGroup && !isOutgoing && isFirstInGroup) {
                    val authorColor = com.ghostgram.app.ui.theme.TelegramColors.getColorForUser(message.senderId)

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
                                message = message
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
                if (message.text.isNotBlank()) {
                    val isEncrypted = message.fileExtraInfo == "ENCRYPTED"

                    ChatMessageLayout(
                        text = {
                            Text(
                                text = if (isEncrypted) "🔒 ${message.text}" else message.text,
                                color = if (isEncrypted) GhostSecureGreen else Color.White,
                                // Читаемый размер шрифта Telegram:
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 21.sp
                            )
                        },
                        time = {
                            MessageTimeAndStatus(
                                message = message,
                                textColor = Color.White,
                                iconColor = Color.White
                            )
                        }
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
        /*if (isOutgoing) {
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
        }*/
    }
}

@Composable
fun MessageTimeAndStatus(
    message: Message,
    textColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val timeString = TimeFormatter.formatTime(message.date)

    if (timeString.isNotBlank() || message.isOutgoing) {
        // Делаем цвет времени мягким и приглушенным (как в Telegram!)
        val mutedColor = textColor.copy(alpha = 0.6f)

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ВАРИАНТ А: Полное слово "изменено" (как в Telegram Desktop на твоем скрине)
            if (message.isEdited) {
                Text(
                    text = "изменено",
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal, // Тонкий!
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Время
            Text(
                text = timeString,
                color = mutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            // Галочки / Часики
            if (message.isOutgoing) {
                Spacer(modifier = Modifier.width(3.dp))
                if (message.isSending) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
                        contentDescription = null,
                        tint = mutedColor,
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (message.isRead) androidx.compose.material.icons.Icons.Default.DoneAll else androidx.compose.material.icons.Icons.Default.Done,
                        contentDescription = null,
                        tint = if (message.isRead) Color(0xFF4FC3F7) else iconColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(12.dp) // 12dp сидит идеально по высоте с 11.sp шрифтом!
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageLayout(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    time: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            text()
            time()
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val timePlaceable = measurables[1].measure(Constraints())

        val spacing = 7.dp.roundToPx()

        // Проверяем: это одна строка? (Высота 1 строки <= 24dp)
        val isSingleLine = textPlaceable.height <= 28.dp.roundToPx()
        val fitsOnSingleLine = isSingleLine && (textPlaceable.width + spacing + timePlaceable.width <= constraints.maxWidth)

        if (fitsOnSingleLine) {
            // ОДНА СТРОКА: Высота пузыря СТРОГО равна высоте текста! Никаких раздуваний!
            val totalWidth = textPlaceable.width + spacing + timePlaceable.width
            val totalHeight = textPlaceable.height

            // Время сажаем на 2dp ниже центра, чтобы оно стояло на одной линии с буквами
            val timeY = (totalHeight - timePlaceable.height - 2.dp.roundToPx()).coerceAtLeast(0)

            layout(totalWidth, totalHeight) {
                // Текст строго в начале
                textPlaceable.placeRelative(0, 0)
                // Время справа внизу
                timePlaceable.placeRelative(textPlaceable.width + spacing, timeY)
            }
        } else {
            // МНОГОСТРОЧНЫЙ ТЕКСТ: Время под текстом справа
            val totalWidth = maxOf(textPlaceable.width, timePlaceable.width)
            val totalHeight = textPlaceable.height + timePlaceable.height + 2.dp.roundToPx()

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(totalWidth - timePlaceable.width, textPlaceable.height + 2.dp.roundToPx())
            }
        }
    }
}
