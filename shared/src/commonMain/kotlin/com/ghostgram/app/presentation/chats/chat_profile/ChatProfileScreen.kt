package com.ghostgram.app.presentation.chats.chat_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.openVideoInSystemPlayer
import entity.ChatFullProfile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatProfileRoute(
    viewModel: ChatProfileViewModel = koinViewModel(),
    onBackClick: () -> Unit
){
    val state by viewModel.state.collectAsState()
    val profile = state.profile

    ChatProfileScreen(
        state = state,
        profile = profile,
        onTabSelected = {viewModel.onTabSelected(it)},
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileScreen(
    state: ChatProfileState,
    profile: ChatFullProfile?,
    onTabSelected: (Int) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground)
            )
        }
    ) { innerPadding ->
        if (state.isLoading || profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 💥 1. БОЛЬШАЯ АВАТАРКА ПРОФИЛЯ
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(GhostPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarPath.isNullOrBlank()) {
                        val model = if (profile.avatarPath!!.startsWith("/")) "file://${profile.avatarPath}" else profile.avatarPath
                        AsyncImage(
                            model = model,
                            contentDescription = "Аватарка",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = profile.title.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Имя
                Text(
                    text = profile.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // Подзаголовок
                Text(
                    text = if (profile.isGroup) "группа" else "в сети",
                    color = GhostAccentGreen,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 💥 2. БЛОК ИНФОРМАЦИИ (Телефон, Юзернейм, Био)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GhostCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (profile.phoneNumber.isNotBlank()) {
                        InfoRow(title = "Телефон", value = "+${profile.phoneNumber}")
                    }
                    if (profile.username.isNotBlank()) {
                        InfoRow(title = "Имя пользователя", value = "@${profile.username}")
                    }
                    if (profile.bio.isNotBlank()) {
                        InfoRow(title = "О себе", value = profile.bio)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 💥 3. ВКЛАДКИ ОБЩИХ МЕДИА (Задел под Этап 2!)
                TabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = GhostPrimary,
                    divider = {}
                ) {
                    Tab(selected = state.selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text("Медиа") })
                    Tab(selected = state.selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text("Файлы") })
                    Tab(selected = state.selectedTab == 2, onClick = { onTabSelected(2) }, text = { Text("Голосовые") })
                }

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoadingMedia) {
                        CircularProgressIndicator(color = GhostPrimary, modifier = Modifier.size(32.dp))
                    } else if (state.sharedMedia.isEmpty()) {
                        Text("Нет общих файлов", color = GhostTextSecondary, fontSize = 14.sp)
                    } else {
                        when (state.selectedTab) {
                            // 💥 ВКЛАДКА 0: СЕТКА ФОТО И ВИДЕО (3 В РЯД)
                            0 -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.sharedMedia, key = { it.id }) { media ->
                                        val model = media.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }

                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f) // Строго квадратная ячейка!
                                                .background(GhostCard)
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    if (media.mediaType == entity.MessageMediaType.VIDEO && !media.fileName.isNullOrBlank()) {
                                                        openVideoInSystemPlayer(media.fileName!!)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (model != null) {
                                                AsyncImage(
                                                    model = model,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(if (media.mediaType == entity.MessageMediaType.VIDEO) "🎬" else "📷", fontSize = 24.sp)
                                            }

                                            // Если это видео — рисуем плашку хронометража в углу
                                            if (media.mediaType == entity.MessageMediaType.VIDEO) {
                                                Text(
                                                    text = media.fileExtraInfo ?: "0:00",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 💥 ВКЛАДКА 1: СПИСОК ДОКУМЕНТОВ
                            1 -> {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.sharedMedia, key = { it.id }) { doc ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GhostCard).padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = GhostPrimary, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(doc.fileName ?: "Документ", color = Color.White, fontSize = 14.sp, maxLines = 1)
                                                Text(com.ghostgram.app.utils.TimeFormatter.formatDateHeader(doc.date), color = GhostTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // 💥 ВКЛАДКА 2: ГОЛОСОВЫЕ СООБЩЕНИЯ
                            2 -> {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.sharedMedia, key = { it.id }) { voice ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GhostCard).padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = GhostAccentGreen, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text("Голосовое сообщение", color = Color.White, fontSize = 14.sp)
                                                Text(voice.fileExtraInfo ?: "0:00", color = GhostTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
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
private fun InfoRow(title: String, value: String) {
    Column {
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = title, color = GhostTextSecondary, fontSize = 12.sp)
    }
}