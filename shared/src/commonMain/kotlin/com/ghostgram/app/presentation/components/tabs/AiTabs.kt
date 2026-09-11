package com.ghostgram.app.presentation.components.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated

@Composable
fun AiTabs(
    smartReplies: List<String>,
    isCatchUpLoading: Boolean,
    isRepliesLoading: Boolean,
    unreadCount: Int,
    onIntent: (ChatDetailsIntent) -> Unit,
) {
    var expandedAiMenu by remember { mutableStateOf(false) }

    // Если ИИ уже сгенерировал ответы, показываем их. Иначе - одну кнопку меню.
    if (smartReplies.isNotEmpty()) {
        // Показываем сгенерированные ответы (они заменяют кнопку ИИ, пока юзер не выберет один)
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(smartReplies) { reply ->
                SuggestionChip(
                    onClick = { onIntent(ChatDetailsIntent.OnSmartReplyClick(reply)) },
                    label = { Text(reply, color = Color.White) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = GhostPrimary.copy(
                            alpha = 0.2f
                        )
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = GhostPrimary
                    )
                )
            }
            item {
                IconButton(
                    onClick = { onIntent(ChatDetailsIntent.OnSmartReplyClick("")) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("✖️", fontSize = 12.sp)
                }
            }
        }
    } else {
        // 💥 ОДНА ЕДИНСТВЕННАЯ КНОПКА!
        Box(modifier = Modifier.padding(bottom = 8.dp)) {
            SuggestionChip(
                onClick = { expandedAiMenu = true },
                label = {
                    Text(
                        text = if (isCatchUpLoading || isRepliesLoading) "🧠 ИИ думает..." else "✨ Ghost AI",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = GhostCard),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = GhostPrimary.copy(alpha = 0.5f)
                ),
                enabled = !isCatchUpLoading && !isRepliesLoading
            )

            // 💥 ВЫПАДАЮЩЕЕ МЕНЮ
            DropdownMenu(
                expanded = expandedAiMenu,
                onDismissRequest = { expandedAiMenu = false },
                modifier = Modifier.background(GhostSurfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("⚡️ Catch Up (Пропущено: ${unreadCount})", color = Color.White) },
                    onClick = {
                        expandedAiMenu = false
                        onIntent(ChatDetailsIntent.OnCatchUpClick)
                    }
                )
                DropdownMenuItem(
                    text = { Text("💡 Сгенерировать ответы", color = Color.White) },
                    onClick = {
                        expandedAiMenu = false
                        onIntent(ChatDetailsIntent.OnGenerateRepliesClick)
                    }
                )
            }
        }
    }
}
