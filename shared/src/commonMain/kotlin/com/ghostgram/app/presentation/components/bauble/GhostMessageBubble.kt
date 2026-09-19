package com.ghostgram.app.presentation.components.bauble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State.Empty.painter
import com.ghostgram.app.presentation.components.openVideoInSystemPlayer
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.TgsDecoder
import com.ghostgram.app.utils.TimeFormatter
import entity.Message
import entity.MessageMediaType
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun GhostMessageBubble(
    message: Message,
    chatAvatarPath: String?,
    myAvatarPath: String?, // 💥 Вернули твою аватарку!
    chatTitle: String,
    isFirstInGroup: Boolean = true, // 💥 Новые параметры
    isLastInGroup: Boolean = true,
    replyMessage: Message? = null,
    onLongClick: () -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = message.isOutgoing
    val isVideo = message.mediaType == MessageMediaType.VIDEO || message.mediaType == MessageMediaType.VIDEO_NOTE
    val isSticker = message.mediaType == MessageMediaType.STICKER
    val timeString = TimeFormatter.formatTime(message.date)
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
        return // 💥 Выходим из функции, чтобы не рисовать синий пузырь!
    }

    // Внешний ряд, который держит Аватарки и Пузырь
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isFirstInGroup) 8.dp else 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Аватарки прижаты к низу
    ) {
        // 💥 1. АВАТАРКА СОБЕСЕДНИКА СЛЕВА (Для входящих)
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
                .widthIn(
                    if (isVideo) 240.dp else 80.dp,
                    max = 280.dp
                ) // Ограничиваем только максимум, чтобы он сжимался под текст
                .clip(bubbleShape)
                .combinedClickable( // 💥 ЛОВИМ ДОЛГИЙ ТАП
                    onClick = {},
                    onLongClick = onLongClick
                )
                .then(
                    if (isSticker) {
                        Modifier.background(Color.Transparent) // Стикеры без фона!
                    } else if (isOutgoing) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    GhostPrimary,
                                    GhostSecondary
                                )
                            )
                        )
                    } else {
                        Modifier.background(Brush.linearGradient(listOf(GhostCard, GhostCard)))
                    }
                )
                .padding(if (isVideo && !hasText ||isSticker) 0.dp else 10.dp)
        ) {
            Column(
                modifier = Modifier.wrapContentWidth(), // 💥 Пузырь плотно облегает контент
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (message.replyToMessageId != 0L) {
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

                when (message.mediaType) {
                    MessageMediaType.PHOTO -> {
                        if (message.photoPath != null) {
                            val model =
                                if (message.photoPath!!.startsWith("/")) "file://${message.photoPath}" else message.photoPath
                            AsyncImage(
                                model = model,
                                contentDescription = "Фото",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onMediaClick(model ?: "") } // 💥 КЛИКАБЕЛЬНО!
                            )
                        }
                    }

                    MessageMediaType.VIDEO, MessageMediaType.VIDEO_NOTE -> {
                        //val model = message.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }
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
                        /*if (message.photoPath != null) {
                            val model = message.photoPath?.let {
                                if (it.startsWith("/")) "file://$it" else it
                            }
                            Box(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f)) // Темный фон
                                    .clickable {
                                        if (!message.isSending && model != null) onMediaClick(model)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = "Превью видео",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                }

                                // 💥 2. ЕСЛИ ВИДЕО ГРУЗИТСЯ — ПОКАЗЫВАЕМ СПИННЕР!
                                if (message.isSending) {
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
                                            text = "Отправка видео...",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                // 💥 3. КОГДА ЗАГРУЗИЛОСЬ — ПОКАЗЫВАЕМ КНОПКУ PLAY!
                                else {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("▶", color = Color.White, fontSize = 20.sp)
                                    }

                                    // Хронометраж в углу
                                    Text(
                                        text = message.fileExtraInfo ?: "0:00",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }*/
                    }

                    MessageMediaType.DOCUMENT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.2f)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                                    .background(Color(0xFF2F88D4)),
                                contentAlignment = Alignment.Center
                            ) { Text("📄", fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "Файл",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = message.fileExtraInfo ?: "Документ",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    MessageMediaType.STICKER -> {
                        if (message.photoPath != null) {
                            // 1. Состояние для распакованного JSON
                            var tgsJson by remember { mutableStateOf<String?>(null) }

                            // 2. Распаковываем файл в фоне при появлении на экране
                            LaunchedEffect(message.photoPath) {
                                tgsJson = TgsDecoder.decodeTgsToJson(message.photoPath!!)
                            }
                            if (tgsJson != null) {
                                // 1. Парсим JSON
                                val composition by rememberLottieComposition(
                                    spec = LottieCompositionSpec.JsonString(tgsJson!!)
                                )

                                // 💥 2. Создаем независимый стейт анимации (Крутим бесконечно)
                                val progress by animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = Int.MAX_VALUE // Вместо красного Compottie.IterateForever
                                )

                                // 💥 3. Передаем прогресс в отрисовщик
                                val painter = rememberLottiePainter(
                                    composition = composition,
                                    progress = { progress }
                                )

                                androidx.compose.foundation.Image(
                                    painter = painter,
                                    contentDescription = "Анимированный стикер",
                                    modifier = Modifier.size(140.dp)
                                )
                            } else {
                                // Лоадер, пока распаковывается GZIP
                                Box(
                                    modifier = Modifier.size(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(
                                            24.dp
                                        ), color = GhostPrimary
                                    )
                                }
                            }
                        }
                    }

                    MessageMediaType.VOICE -> {
                        // ... твой виджет голосового ...
                    }

                    MessageMediaType.TEXT -> { /* Обычный текст, ничего не делаем */
                    }
                }

                // 📄 Виджет документа
                /*if (message.mediaType == MessageMediaType.DOCUMENT) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape)
                                .background(Color(0xFF2F88D4)),
                            contentAlignment = Alignment.Center
                        ) { Text("📄", fontSize = 20.sp) }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.fileName ?: "Документ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = message.fileExtraInfo ?: "Файл",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
                            )
                        }
                    }
                }*/
                // 🗑 Метка Anti-Revoke
                if (message.isDeletedLocally) {
                    Text(
                        text = "🗑️ Удалено отправителем",
                        color = GhostAccentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (message.text.isNotBlank() && !isSticker) {
                    val textToDisplay = if (message.fileExtraInfo == "ENCRYPTED") "🔒 ${message.text}" else message.text

                    ChatMessageLayout(
                        text = {
                            Text(
                                text = textToDisplay,
                                color = if (message.fileExtraInfo == "ENCRYPTED") GhostSecureGreen else Color.White,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        },
                        time = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = timeString, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                if (isOutgoing) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = "Статус",
                                        tint = if (message.isRead) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
            if (isSticker || (message.photoPath != null && message.text.isBlank())) {
                if (timeString.isNotBlank() || isOutgoing) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = timeString, color = Color.White, fontSize = 11.sp)
                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            /*Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = "Статус",
                                tint = if (message.isRead) Color(0xFF4FC3F7) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )*/
                            if (message.isSending) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Отправляется",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                // Когда отправилось — наши привычные галочки
                                Icon(
                                    imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                    contentDescription = "Статус",
                                    tint = if (message.isRead) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
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

        val spacing = 8.dp.roundToPx()
        // Однострочный текст при 15.sp имеет высоту около 20-24.dp (строго <= 28.dp)
        val isSingleLine = textPlaceable.height <= 28.dp.roundToPx()
        val fitsOnSingleLine = isSingleLine && (textPlaceable.width + spacing + timePlaceable.width <= constraints.maxWidth)

        if (fitsOnSingleLine) {
            // 💥 КОРОТКИЙ ТЕКСТ («ку», «ок», «привет»): СТРОГО В ОДНУ ЛИНИЮ!
            // Текст идет слева (x=0), а время — СПРАВА от него через отступ spacing.
            val totalWidth = textPlaceable.width + spacing + timePlaceable.width
            val totalHeight = maxOf(textPlaceable.height, timePlaceable.height)

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(0, (totalHeight - textPlaceable.height) / 2)
                timePlaceable.placeRelative(textPlaceable.width + spacing, totalHeight - timePlaceable.height)
            }
        } else {
            // 💥 МНОГОСТРОЧНЫЙ ИЛИ ДЛИННЫЙ ТЕКСТ: ВРЕМЯ СТРОГО ПОД ТЕКСТОМ СПРАВА!
            val totalWidth = maxOf(textPlaceable.width, timePlaceable.width)
            val totalHeight = textPlaceable.height + timePlaceable.height + 2.dp.roundToPx()

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(0, 0)
                timePlaceable.placeRelative(totalWidth - timePlaceable.width, textPlaceable.height + 2.dp.roundToPx())
            }
        }
    }
}