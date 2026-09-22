package com.ghostgram.app.presentation.components.bottom_sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendSticker
import com.ghostgram.app.ui.theme.GhostCard
import entity.TelegramSticker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerBottomSheet(
    stickers: List<TelegramSticker>,
    onDismiss: () -> Unit,
    onIntent: (ChatDetailsIntent) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GhostCard,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Недавние стикеры",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (stickers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                    columns = Adaptive(minSize = 64.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stickers, key = { it.fileId }) { sticker ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onIntent(OnSendSticker(sticker.fileId)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!sticker.thumbnailPath.isNullOrBlank()) {
                                val model = if (sticker.thumbnailPath!!.startsWith("/")) "file://${sticker.thumbnailPath}" else sticker.thumbnailPath
                                AsyncImage(
                                    model = model,
                                    contentDescription = sticker.emoji,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(text = sticker.emoji, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}