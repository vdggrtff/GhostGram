package com.ghostgram.app.presentation.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun GhostTopBar(
    avatarPath: String?,
    chatTitle: String,
    isGhostMode: Boolean,
    onBackClick: () -> Unit,
    onIntent: (ChatDetailsIntent) -> Unit
){
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GhostBackground,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 💥 Аватарка собеседника в шапке
                if (avatarPath != null) {
                    AsyncImage(
                        model = avatarPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GhostPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chatTitle.take(1).uppercase(),
                            color = GhostPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chatTitle,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text("Online • Ghost Core", fontSize = 12.sp, color = GhostPrimary)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            IconButton(
                onClick = { onIntent(ChatDetailsIntent.OnToggleGhostMode) }
            ) {
                if (isGhostMode) {
                    // Режим невидимки включен (Фиолетовый неоновый призрак)
                    Text("👻", fontSize = 22.sp)
                } else {
                    // Обычный режим (Глаз, тебя видят!)
                    Text("👁️", fontSize = 20.sp)
                }
            }
        }
    )
}