package com.ghostgram.app.presentation.components.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostPrimary
import entity.ActiveCall
import entity.CallStatus

@Composable
fun CallOverlayScreen(
    call: ActiveCall,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    // 💥 ПОЛНОЭКРАННЫЙ DIALOG: НАКРЫВАЕТ ВСЁ ПРИЛОЖЕНИЕ, НЕ ЛОМАЯ НИЖНИЙ ЛАЙАУТ!
    Dialog(
        onDismissRequest = { /* Звонок нельзя закрыть кликом в пустоту */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // На весь экран
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (call.status == CallStatus.ACTIVE || call.status == CallStatus.OUTGOING_RINGING) 1.25f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F141C))
        ) {
            // Размытый аватар
            if (call.userAvatar != null) {
                val model = if (call.userAvatar!!.startsWith("/")) "file://${call.userAvatar}" else call.userAvatar
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.18f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 4 ЭМОДЗИ БЕЗОПАСНОСТИ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒 E2EE Звонок", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)

                    if (call.emojis.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, GhostAccentGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            call.emojis.forEach { emoji ->
                                Text(text = emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // ЦЕНТР: Аватар и статус
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(GhostPrimary.copy(alpha = 0.2f))
                        )

                        if (call.userAvatar != null) {
                            val model = if (call.userAvatar!!.startsWith("/")) "file://${call.userAvatar}" else call.userAvatar
                            AsyncImage(
                                model = model,
                                contentDescription = "Аватар",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(120.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(120.dp).clip(CircleShape).background(GhostPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = call.userName.take(1).uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = call.userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val statusText = when (call.status) {
                        CallStatus.OUTGOING_RINGING -> "Идут гудки..."
                        CallStatus.INCOMING_RINGING -> "Входящий звонок..."
                        CallStatus.CONNECTING -> "Соединение..."
                        CallStatus.ACTIVE -> formatCallDuration(call.durationSeconds)
                        CallStatus.ENDED -> "Звонок завершен"
                    }
                    Text(text = statusText, color = GhostAccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                // КНОПКИ УПРАВЛЕНИЯ
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (call.status == CallStatus.INCOMING_RINGING) {
                        // Сбросить
                        IconButton(
                            onClick = onDiscard,
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(GhostAccentRed)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Отклонить", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        // Принять
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(GhostAccentGreen)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Ответить", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        // Микрофон
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(if (call.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(if (call.isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = if (call.isMuted) Color.Black else Color.White)
                        }

                        // Сброс
                        IconButton(
                            onClick = onDiscard,
                            modifier = Modifier.size(68.dp).clip(CircleShape).background(GhostAccentRed)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Сбросить", tint = Color.White, modifier = Modifier.size(34.dp))
                        }

                        // Динамик
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(if (call.isSpeakerOn) GhostPrimary else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Динамик", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
}