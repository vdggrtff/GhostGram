package com.ghostgram.app.presentation.components.bauble.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.GhostAudioPlayer
import com.ghostgram.app.utils.WaveformDecoder
import entity.Message

@Composable
fun VoiceMessageContent(message: Message) {
    val isOutgoing = message.isOutgoing
    val audioPath = message.fileName // Путь к файлу из БД

    val player = remember { GhostAudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    var currentMs by remember { mutableStateOf(0) }
    var totalMs by remember { mutableStateOf(0) }

    val waveformBars = remember(message.waveform) {
        WaveformDecoder.decode(message.waveform, targetBarCount = 36)
    }

    // Останавливаем музыку, если вышли из чата
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    val progress = if (totalMs > 0) (currentMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Row(
        modifier = Modifier
            .width(230.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💥 1. КНОПКА PLAY / PAUSE
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.25f) else GhostPrimary)
                .clickable {
                    if (!audioPath.isNullOrBlank()) {
                        if (isPlaying) {
                            player.stop()
                            isPlaying = false
                            currentMs = 0
                        } else {
                            isPlaying = true
                            player.play(
                                filePath = audioPath,
                                onProgress = { cur, tot ->
                                    currentMs = cur
                                    totalMs = tot
                                },
                                onFinished = {
                                    isPlaying = false
                                    currentMs = 0
                                }
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (audioPath.isNullOrBlank() && !message.isSending) {
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

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {

            // 💥 2. ДВУХЦВЕТНАЯ ЗВУКОВАЯ ВОЛНА НА CANVAS (С перемоткой по тапу!)
            val playedColor = if (isOutgoing) Color.White else GhostPrimary
            val unplayedColor = if (isOutgoing) Color.White.copy(alpha = 0.4f) else GhostTextSecondary.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(totalMs) {
                        detectTapGestures { tapOffset ->
                            if (totalMs > 0) {
                                val newProgress = (tapOffset.x / size.width).coerceIn(0f, 1f)
                                player.seekTo(newProgress)
                                currentMs = (totalMs * newProgress).toInt()
                            }
                        }
                    }
            ) {
                val barWidth = 2.5.dp.toPx()
                val totalWidth = size.width
                val barSpacing = (totalWidth - (waveformBars.size * barWidth)) / (waveformBars.size - 1).coerceAtLeast(1)
                val canvasHeight = size.height

                waveformBars.forEachIndexed { index, amplitude ->
                    val barHeight = (canvasHeight * amplitude).coerceAtLeast(3.dp.toPx())
                    val x = index * (barWidth + barSpacing)
                    val y = (canvasHeight - barHeight) / 2 // Центрируем по вертикали

                    val barProgress = index.toFloat() / waveformBars.size
                    val color = if (barProgress <= progress) playedColor else unplayedColor

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 💥 3. ДИНАМИЧЕСКИЙ ТАЙМЕР: "0:04 / 0:18" при проигрывании или "0:18" в покое!
            val timeText = if (isPlaying && totalMs > 0) {
                val curSec = currentMs / 1000
                val totSec = totalMs / 1000
                "${formatSec(curSec)} / ${formatSec(totSec)}"
            } else {
                message.fileExtraInfo ?: "0:00"
            }

            Text(
                text = timeText,
                color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else GhostTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

private fun formatSec(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${if (s < 10) "0$s" else "$s"}"
}