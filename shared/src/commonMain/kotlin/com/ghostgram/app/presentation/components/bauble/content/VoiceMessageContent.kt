package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.utils.GhostAudioPlayer
import entity.Message

@Composable
fun VoiceMessageContent(message: Message) {
    val isOutgoing = message.isOutgoing
    val audioPath = message.fileName // Путь к файлу из БД

    val player = remember { GhostAudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    // Останавливаем музыку, если вышли из чата
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    Row(
        modifier = Modifier
            .width(220.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💥 ИНТЕРАКТИВНАЯ КНОПКА PLAY / PAUSE
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.2f) else com.ghostgram.app.ui.theme.GhostPrimary)
                .clickable {
                    if (!audioPath.isNullOrBlank()) {
                        if (isPlaying) {
                            player.stop()
                            isPlaying = false
                        } else {
                            isPlaying = true
                            player.play(audioPath) {
                                isPlaying = false // Закончил играть
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (audioPath.isNullOrBlank() && !message.isSending) {
                // Если файл еще качается из Telegram
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = if (isPlaying) 0.dp else 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Звуковая волна и время
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val barColor = if (isOutgoing) Color.White.copy(alpha = 0.7f) else com.ghostgram.app.ui.theme.GhostTextSecondary
                for (i in 0..17) {
                    val height = remember { (4..20).random().dp }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.fileExtraInfo ?: "0:00",
                color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else com.ghostgram.app.ui.theme.GhostTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}