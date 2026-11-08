package com.ghostgram.app.presentation.components.bauble.layout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.player.TgsEffectPlayer
import com.ghostgram.app.ui.theme.GhostPrimary
import entity.MessageReactionItem

private val customEmojiPathCache = mutableMapOf<Long, String>()

@Composable
fun CustomEmojiIcon(
    customEmojiId: Long,
    onLoadPath: suspend (Long) -> String?,
    modifier: Modifier = Modifier
) {
    var localPath by remember(customEmojiId) {
        mutableStateOf(customEmojiPathCache[customEmojiId])
    }

    // 💥 АВТОМАТИЧЕСКИ КАЧАЕМ КАРТИНКУ ПРИ ПОЯВЛЕНИИ КАПСУЛЫ:
    LaunchedEffect(customEmojiId) {
        if (localPath == null) {
            val path = onLoadPath(customEmojiId)
            if (path != null) {
                customEmojiPathCache[customEmojiId] = path
                localPath = path
            }
        }
    }

    if (!localPath.isNullOrBlank()) {
        val model = if (localPath!!.startsWith("/")) "file://$localPath" else localPath
        AsyncImage(
            model = model,
            contentDescription = "Кастомный эмодзи",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        // Пока файл качается (доли секунды) — горит искорка
        Text(text = "✨", fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsRow(
    reactions: List<MessageReactionItem>,
    isOutgoing: Boolean,
    authorAvatarPath: String? = null,
    isBursting: Boolean = false,
    burstEffectJson: String? = null,
    burstEmoji: String? = null,
    onBurstFinished: () -> Unit = {},
    onReactionClick: (emoji: String, customEmojiId: Long, isChosen: Boolean) -> Unit,
    onLoadCustomEmoji: suspend (Long) -> String? = { null },
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier = modifier.padding(
            top = 4.dp,
            start = if (isOutgoing) 0.dp else 4.dp,
            end = if (isOutgoing) 4.dp else 0.dp
        ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { item ->
            val isChosen = item.isChosen
            var isPressed by remember { mutableStateOf(false) }

            val spawnScale = remember { androidx.compose.animation.core.Animatable(0.4f) }
            LaunchedEffect(item.count, item.isChosen) {
                spawnScale.snapTo(0.6f)
                spawnScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }

            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                finishedListener = { isPressed = false },
                label = "press_scale"
            )

            val capsuleBg = if (isChosen) GhostPrimary else Color(0xFF1E2430)
            val capsuleBorder = if (isChosen) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.14f)

            // 💥 КОНТЕЙНЕР КОНКРЕТНОЙ КАПСУЛЫ:
            Box(contentAlignment = Alignment.Center) {
                // 1. Сама капсула реакции
                Row(
                    modifier = Modifier
                        .scale(spawnScale.value * pressScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(capsuleBg)
                        .border(1.dp, capsuleBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            isPressed = true
                            onReactionClick(item.emoji, item.customEmojiId, isChosen)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.customEmojiId != 0L) {
                        CustomEmojiIcon(
                            customEmojiId = item.customEmojiId,
                            onLoadPath = onLoadCustomEmoji,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(text = item.emoji, fontSize = 14.sp)
                    }

                    // Микро-аватарка
                    if (item.count <= 2) {
                        val avatarUrl = if (isChosen) null else authorAvatarPath
                        if (!avatarUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val model = if (avatarUrl.startsWith("/")) "file://$avatarUrl" else avatarUrl
                            AsyncImage(
                                model = model,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                            )
                        }
                    }

                    // Счетчик
                    if (item.count > 1 || item.isChosen || authorAvatarPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.count.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 💥 2. САЛЮТ ЦЕНТРИРУЕТСЯ СТРОГО НАД ЭТОЙ КАПСУЛОЙ!
                // Проверяем: совпадает ли эмодзи этой капсулы с нажатым эмодзи?
                val isThisCapsuleBursting = isBursting && !burstEffectJson.isNullOrBlank() && (
                        (item.customEmojiId != 0L && (burstEmoji.isNullOrBlank() || burstEmoji == "custom" || burstEmoji == item.emoji)) ||
                                (item.emoji.isNotBlank() && item.emoji == burstEmoji)
                        )

                if (isThisCapsuleBursting) {
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .wrapContentSize(align = Alignment.Center, unbounded = true)
                    ) {
                        TgsEffectPlayer(
                            tgsJson = burstEffectJson!!,
                            onFinished = onBurstFinished,
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }
            }
        }
    }
}

