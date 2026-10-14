package com.ghostgram.app.presentation.components.bauble.layout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import entity.MessageReactionItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsRow(
    reactions: List<MessageReactionItem>,
    isOutgoing: Boolean,
    authorAvatarPath: String? = null,
    onReactionClick: (emoji: String, isChosen: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier = modifier.padding(top = 4.dp, start = if (isOutgoing) 0.dp else 4.dp, end = if (isOutgoing) 4.dp else 0.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { item ->
            val isChosen = item.isChosen
            var isPressed by remember { mutableStateOf(false) }

            // 💥 Пружинящая анимация при тапе
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                finishedListener = { isPressed = false },
                label = "reaction_scale"
            )

            // 💥 КОНТРАСТНЫЙ ТЕЛЕГРАМ-ДИЗАЙН:
            // Своя реакция — ярко горит GhostPrimary!
            // Чужая — солидный темный графит с легкой неоновой обводкой, чтобы не сливаться!
            val capsuleBg = if (isChosen) GhostPrimary else Color(0xFF1E2430)
            val capsuleBorder = if (isChosen) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.14f)
            val textColor = Color.White

            Row(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(capsuleBg)
                    .border(1.dp, capsuleBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        isPressed = true
                        onReactionClick(item.emoji, isChosen)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Сам эмодзи
                Text(text = item.emoji, fontSize = 14.sp)

                // 💥 МИКРО-АВАТАРКА КАК В TELEGRAM (Если реакций мало: 1-2 человека):
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

                // Счетчик (если больше 1 или если не влезла аватарка)
                if (item.count > 1 || item.isChosen || authorAvatarPath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.count.toString(),
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

