package com.ghostgram.app.presentation.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostAccentRed
import entity.Message
import entity.MessageMediaType
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TelegramContextMenu(
    msg: Message,
    availableReactions: List<String>,
    messagePosition: Offset,
    clipboardManager: ClipboardManager,
    onIntent: (ChatDetailsIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Стейты полета
    var flyingEmoji by remember { mutableStateOf<String?>(null) }
    var flyingStartOffset by remember { mutableStateOf(Offset.Zero) }
    var isMenuDismissing by remember { mutableStateOf(false) }
    val hopProgress = remember { Animatable(0f) }

    Dialog(
        onDismissRequest = { if (flyingEmoji == null) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart // 💥 АБСОЛЮТНАЯ СИСТЕМА КООРДИНАТ (0, 0)!
        ) {
            // 1. ПОЛУПРОЗРАЧНЫЙ ФОН
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (flyingEmoji != null) 0.08f else 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (flyingEmoji == null) onDismiss() }
                    )
            )

            // 💥 2. САМ ПОЛЕТ ЭМОДЗИ (ПРИЦЕЛЬНЫЙ ПЕРЕЛЕТ ИЗ МЕНЮ В СООБЩЕНИЕ!):
            if (flyingEmoji != null) {
                val progress = hopProgress.value

                // Цель полета: точка капсулы под сообщением!
                val targetX = if (msg.isOutgoing) {
                    messagePosition.x + with(density) { 60.dp.toPx() }
                } else {
                    messagePosition.x + with(density) { 24.dp.toPx() }
                }
                val targetY = messagePosition.y + with(density) { 30.dp.toPx() }

                // Траектория Безье:
                val arcHeightPx = with(density) { 60.dp.toPx() }
                val curX = flyingStartOffset.x + (targetX - flyingStartOffset.x) * progress
                val curY = flyingStartOffset.y + (targetY - flyingStartOffset.y) * progress - (sin(progress * PI) * arcHeightPx).toFloat()

                val currentScale = 1f + sin(progress * PI).toFloat() * 0.5f
                val rotationAngle = sin(progress * PI * 2) * 12f

                Box(
                    modifier = Modifier
                        .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                        .scale(currentScale)
                        .rotate(rotationAngle.toFloat())
                ) {
                    Text(text = flyingEmoji!!, fontSize = 32.sp)
                }
            }

            // 💥 3. ПАРЯЩЕЕ МЕНЮ
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = !isMenuDismissing,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(100))
                ) {
                    Column(
                        horizontalAlignment = if (msg.isOutgoing) Alignment.End else Alignment.Start,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .widthIn(max = 330.dp)
                    ) {
                        // Капсула реакций
                        val emojis = if (availableReactions.isNotEmpty()) availableReactions else listOf("👍", "❤️", "🔥", "😂", "👏", "🤯", "🎉")

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFF1E2430).copy(alpha = 0.98f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            emojis.take(6).forEach { emoji ->
                                var itemOffset by remember { mutableStateOf(Offset.Zero) }
                                var isPressed by remember { mutableStateOf(false) }

                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 1.45f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "emoji_scale"
                                )

                                Text(
                                    text = emoji,
                                    fontSize = 30.sp,
                                    modifier = Modifier
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .onGloballyPositioned { coordinates ->
                                            itemOffset = coordinates.positionInRoot()
                                        }
                                        .clickable {
                                            isPressed = true
                                            // 💥 ФИКСИРУЕМ ТОЧКУ ВЫЛЕТА И ЗАПУСКАЕМ ДУГУ!
                                            flyingEmoji = emoji
                                            flyingStartOffset = itemOffset
                                            isMenuDismissing = true

                                            coroutineScope.launch {
                                                hopProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))

                                                // Приземлился точно в бабл ➔ ставим реакцию и взрываем TGS!
                                                val isChosen = msg.reactions.find { it.emoji == emoji }?.isChosen == true
                                                onIntent(ChatDetailsIntent.OnToggleReaction(msg.id, emoji, isChosen, 0L))
                                                if (!isChosen) {
                                                    onIntent(ChatDetailsIntent.OnPlayReactionEffect(msg.id, emoji))
                                                }
                                                onDismiss()
                                            }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Карточка действий
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(if (msg.text.length > 20) 1f else 0.85f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1E2430).copy(alpha = 0.98f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            ContextMenuItem(icon = Icons.AutoMirrored.Filled.Reply, title = "Ответить") {
                                onIntent(ChatDetailsIntent.OnSwipeToReply(msg))
                                onDismiss()
                            }

                            if (msg.text.isNotBlank()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                ContextMenuItem(icon = Icons.Default.ContentCopy, title = "Копировать текст") {
                                    clipboardManager.setText(AnnotatedString(msg.text))
                                    onDismiss()
                                }
                            }

                            if (msg.isOutgoing && msg.mediaType == MessageMediaType.TEXT) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                ContextMenuItem(icon = Icons.Default.Edit, title = "Редактировать") {
                                    onIntent(ChatDetailsIntent.OnEditMessageClick(msg))
                                    onDismiss()
                                }
                            }

                            if (!msg.isDeletedLocally) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                ContextMenuItem(icon = Icons.Default.Delete, title = "Удалить у всех", tint = GhostAccentRed) {
                                    onIntent(ChatDetailsIntent.OnDeleteMessage(msg.id, revoke = true))
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(icon: ImageVector, title: String, tint: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}