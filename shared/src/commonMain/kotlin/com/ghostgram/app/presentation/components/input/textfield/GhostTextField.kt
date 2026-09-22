package com.ghostgram.app.presentation.components.input.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnInputChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendMessage
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.GhostAudioRecorder
import entity.Message
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import okio.SYSTEM
import kotlin.time.Clock.System

@Composable
fun GhostTextField(
    inputText: String,
    onIntent: (ChatDetailsIntent) -> Unit,
    replyingToMessage: Message?,
    editingMessage: Message?,
    fileLauncher: PickerResultLauncher
) {
    var showStickerPanel by remember { mutableStateOf(false) }
    val recorder = remember { GhostAudioRecorder() }
    var isRecording by remember { mutableStateOf(false) }
    var currentVoicePath by remember { mutableStateOf("") }
    if (replyingToMessage != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GhostSurfaceElevated)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Синяя полоска слева
            Box(modifier = Modifier.width(3.dp).height(32.dp).background(GhostPrimary, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(replyingToMessage.senderName, color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = replyingToMessage.text.ifBlank { "Медиафайл" },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onIntent(ChatDetailsIntent.OnCancelReply) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Отмена", tint = GhostTextSecondary)
            }
        }
    }
    if (editingMessage != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GhostSurfaceElevated)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Зеленая или фиолетовая полоска слева
            Box(modifier = Modifier.width(3.dp).height(32.dp).background(GhostAccentGreen, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))

            // Иконка карандаша
            Icon(Icons.Default.Edit, contentDescription = null, tint = GhostAccentGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Редактирование", color = GhostAccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = editingMessage.text,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { onIntent(ChatDetailsIntent.OnCancelEdit) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Отмена", tint = GhostTextSecondary)
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { fileLauncher.launch() }, // 💥 Открываем системный выбор файлов
            modifier = Modifier.size(42.dp)
                .clip(CircleShape)
                .background(GhostCard)
                .align(Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить", tint = Color.White.copy(alpha = 0.9f) )
        }
        TextField(
            value = inputText,
            onValueChange = { onIntent(OnInputChanged(it)) },
            placeholder = { Text("Сообщение...", color = GhostTextSecondary) },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GhostCard,
                unfocusedContainerColor = GhostCard,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            maxLines = 5,
            // Иконка эмодзи внутри поля ввода (справа)
            trailingIcon = {
                IconButton(onClick = { onIntent(ChatDetailsIntent.OnToggleStickers) }) {
                    Icon(
                        imageVector = Outlined.Face,
                        contentDescription = "Стикеры",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (inputText.isNotBlank()) {
            // 1. ЕСТЬ ТЕКСТ -> КНОПКА "ОТПРАВИТЬ"
            IconButton(
                onClick = { onIntent(ChatDetailsIntent.OnSendMessage) },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GhostPrimary)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // 2. ТЕКСТА НЕТ -> КНОПКА "МИКРОФОН" (С диктофоном!)
            var isRecording by remember { mutableStateOf(false) }
            val recorder = remember { GhostAudioRecorder() } // (Если ты уже написал класс диктофона)
            var currentVoicePath by remember { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) GhostAccentRed else GhostCard)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // 💥 1. ЗАЖАЛИ (НАЧАЛО ЗАПИСИ)
                                isRecording = true
                                val tempDir = okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ghostgram_temp"
                                if (!okio.FileSystem.SYSTEM.exists(tempDir)) okio.FileSystem.SYSTEM.createDirectories(tempDir)

                                // Формируем путь (сохраняем в .wav, так как стандартная Java пишет в WAV)
                                currentVoicePath = (tempDir / "voice_${System.now().toEpochMilliseconds()}.wav").toString()

                                recorder.startRecording(currentVoicePath)
                                // (Опционально) onIntent(ChatDetailsIntent.OnStartRecording(currentVoicePath))

                                // 💥 ЖДЕМ, ПОКА ОТПУСТИТ МЫШКУ ИЛИ ПАЛЕЦ...
                                val success = tryAwaitRelease()

                                // 💥 2. ОТПУСТИЛИ (КОНЕЦ ЗАПИСИ И ОТПРАВКА)
                                isRecording = false
                                recorder.stopRecording()

                                if (success) {
                                    onIntent(ChatDetailsIntent.OnStopRecording(send = true, filePath = currentVoicePath))
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) androidx.compose.material.icons.Icons.Default.MicNone else androidx.compose.material.icons.Icons.Default.Mic,
                    contentDescription = "Голосовое",
                    tint = if (isRecording) Color.White else GhostTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        /*IconButton(
            onClick = { onIntent(OnSendMessage) },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GhostPrimary) // На макете она залита сплошным фиолетовым
        ) {
            Icon(
                imageVector = Filled.Send,
                contentDescription = "Отправить",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }*/
    }
}