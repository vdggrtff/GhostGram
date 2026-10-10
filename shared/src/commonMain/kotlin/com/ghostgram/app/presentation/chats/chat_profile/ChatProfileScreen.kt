package com.ghostgram.app.presentation.chats.chat_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.dialog.FullScreenImageDialog
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import com.ghostgram.app.utils.GhostAudioPlayer
import com.ghostgram.app.utils.openVideoInSystemPlayer
import entity.Message
import entity.MessageMediaType
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatProfileRoute(
    viewModel: ChatProfileViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onNavigateToMessage: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    ChatProfileScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onToggleMute = viewModel::onToggleMute,
        onBackClick = onBackClick,
        onNavigateToMessage = onNavigateToMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileScreen(
    state: ChatProfileState,
    onTabSelected: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onNavigateToMessage: (Long) -> Unit,
    onBackClick: () -> Unit,
) {
    val profile = state.profile

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedMediaForMenu by remember { mutableStateOf<Message?>(null) }

    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Опции профиля */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground)
            )
        }
    ) { innerPadding ->
        if (state.isLoading || profile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GhostPrimary)
            }
        } else {
            // 💥 ЕДИНЫЙ СКРОЛЛ ДЛЯ ВСЕГО ЭКРАНА ЧЕРЕЗ GRID СО SPAN!
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                // 💥 1. ШАПКА: АВАТАРКА + ИМЯ + СТАТУС (Занимает все 3 колонки)
                item(span = { GridItemSpan(3) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Большая круглая аватарка
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(GhostPrimary)
                                .clickable {
                                    if (!profile.avatarPath.isNullOrBlank()) {
                                        fullScreenImageUrl = profile.avatarPath
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile.avatarPath.isNullOrBlank()) {
                                val model =
                                    if (profile.avatarPath!!.startsWith("/")) "file://${profile.avatarPath}" else profile.avatarPath
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
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Имя
                        Text(
                            text = profile.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Статус
                        Text(
                            text = if (profile.isGroup) "группа" else "в сети",
                            color = if (profile.isGroup) GhostTextSecondary else GhostAccentGreen,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 💥 2. РЯД ИЗ 4 КНОПОК КАК В TELEGRAM
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileActionButton(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                label = "Сообщение",
                                onClick = onBackClick
                            )
                            ProfileActionButton(
                                icon = if (state.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                label = if (state.isMuted) "Без звука" else "Звук",
                                onClick = onToggleMute
                            )
                            ProfileActionButton(
                                icon = Icons.Default.Search,
                                label = "Поиск",
                                onClick = onBackClick
                            )
                            ProfileActionButton(
                                icon = Icons.Default.CardGiftcard,
                                label = "Подарок",
                                onClick = { /* TODO Gift */ })
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 💥 3. БЛОК ИНФОРМАЦИИ (Username, Телефон, Био)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GhostCard)
                                .padding(vertical = 8.dp)
                        ) {
                            if (profile.username.isNotBlank()) {
                                ProfileInfoItem(
                                    title = "@${profile.username}",
                                    subtitle = "Имя пользователя",
                                    icon = Icons.Default.QrCode,
                                    onClick = { clipboardManager.setText(AnnotatedString("@${profile.username}")) }
                                )
                            }
                            if (profile.phoneNumber.isNotBlank()) {
                                HorizontalDivider(
                                    color = GhostBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                ProfileInfoItem(
                                    title = "+${profile.phoneNumber}",
                                    subtitle = "Мобильный",
                                    onClick = { clipboardManager.setText(AnnotatedString("+${profile.phoneNumber}")) }
                                )
                            }
                            if (profile.bio.isNotBlank()) {
                                HorizontalDivider(
                                    color = GhostBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                ProfileInfoItem(title = profile.bio, subtitle = "О себе")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 💥 4. ВКЛАДКИ-ПИЛЮЛИ (КАК В TELEGRAM!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelegramTabPill(
                                title = "Медиа",
                                isSelected = state.selectedTab == 0,
                                onClick = { onTabSelected(0) })
                            TelegramTabPill(
                                title = "Файлы",
                                isSelected = state.selectedTab == 1,
                                onClick = { onTabSelected(1) })
                            TelegramTabPill(
                                title = "Голосовые",
                                isSelected = state.selectedTab == 2,
                                onClick = { onTabSelected(2) })
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 💥 5. КОНТЕНТ ВКЛАДОК
                if (state.isLoadingMedia) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GhostPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else if (state.sharedMedia.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет файлов", color = GhostTextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    when (state.selectedTab) {
                        // 💥 Вкладка 0: Сетка фото и видео (3 в ряд, 1 колонка на каждый)
                        0 -> {
                            val mediaList = state.sharedMedia.filter {
                                it.mediaType == MessageMediaType.PHOTO || it.mediaType == MessageMediaType.VIDEO
                            }
                            items(mediaList, key = { it.id }) { media ->
                                val model =
                                    media.photoPath?.let { if (it.startsWith("/")) "file://$it" else it }
                                val isVideo = media.mediaType == MessageMediaType.VIDEO

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f) // Квадрат
                                        .background(GhostCard)
                                        .combinedClickable(
                                            onClick = {
                                                if (isVideo && !media.fileName.isNullOrBlank()) {
                                                    openVideoInSystemPlayer(
                                                        media.fileName!!
                                                    )
                                                } else if (model != null) {
                                                    // Открываем фото на весь экран!
                                                    fullScreenImageUrl = model
                                                }
                                            },
                                            onLongClick = {
                                                // Запоминаем для меню "Показать в чате"
                                                selectedMediaForMenu = media
                                            }
                                        ),
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
                                        Icon(
                                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = GhostTextSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    // 💥 Плашка длительности видео в углу: ▶ 0:00 (КАК НА СКРИНЕ TELEGRAM!)
                                    if (isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "▶ ${media.fileExtraInfo ?: "0:00"}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 💥 Вкладка 1: Документы (Занимают всю строку span = 3)
                        1 -> {
                            val docs = state.sharedMedia.filter { it.mediaType == MessageMediaType.DOCUMENT }
                            items(docs, key = { it.id }, span = { GridItemSpan(3) }) { doc ->

                                val pathOrName = doc.fileName ?: "Файл"
                                // Извлекаем чистое имя (всё, что после последнего слэша)
                                val displayName = pathOrName.substringAfterLast('/').substringAfterLast('\\')
                                val extension = displayName.substringAfterLast('.', "").uppercase().take(4)

                                // Если в строке есть слэш — значит это скачанный путь на диске!
                                val isDownloaded = pathOrName.contains("/") || pathOrName.contains("\\")

                                val dateStr = com.ghostgram.app.utils.TimeFormatter.formatDateHeader(doc.date)
                                val sizeStr = doc.fileExtraInfo ?: ""
                                val subtitle = if (isDownloaded) sizeStr else "Скачивание... $sizeStr"

                                val badgeColor = when (extension) {
                                    "APK" -> Color(0xFF43A047)
                                    "PDF" -> Color(0xFFE53935)
                                    "ZIP", "RAR" -> Color(0xFFFB8C00)
                                    else -> GhostPrimary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp)).background(GhostCard)
                                        .combinedClickable(
                                            onClick = {
                                                // 💥 ЕСЛИ СКАЧАНО - ОТКРЫВАЕМ!
                                                if (isDownloaded) {
                                                    com.ghostgram.app.utils.openFileInSystem(pathOrName)
                                                }
                                            },
                                            onLongClick = { selectedMediaForMenu = doc }
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(badgeColor), contentAlignment = Alignment.Center) {
                                        Text(extension.ifBlank { "DOC" }, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(displayName, color = Color.White, fontSize = 14.sp, maxLines = 1)
                                        Text("$subtitle • $dateStr", color = if (isDownloaded) GhostTextSecondary else GhostPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        2 -> {
                            val voices = state.sharedMedia.filter { it.mediaType == MessageMediaType.VOICE }
                            items(voices, key = { it.id }, span = { GridItemSpan(3) }) { voice ->
                                val audioPath = voice.fileName
                                val isDownloaded = !audioPath.isNullOrBlank() && (audioPath.contains("/") || audioPath.contains("\\"))

                                val player = remember { GhostAudioPlayer() }
                                var isPlaying by remember { mutableStateOf(false) }

                                DisposableEffect(Unit) { onDispose { player.stop() } }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp)).background(GhostCard)
                                        .combinedClickable(onClick = {}, onLongClick = { selectedMediaForMenu = voice })
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(GhostAccentGreen)
                                            .clickable {
                                                // 💥 ЕСЛИ СКАЧАНО - ИГРАЕМ!
                                                if (isDownloaded) {
                                                    if (isPlaying) {
                                                        player.stop(); isPlaying = false
                                                    } else {
                                                        isPlaying = true; player.play(audioPath!!, onProgress = { _, _ -> }) { isPlaying = false }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isDownloaded) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = if (isPlaying) 0.dp else 2.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Голосовое сообщение", color = Color.White, fontSize = 14.sp)
                                        Text("${voice.fileExtraInfo} • ${com.ghostgram.app.utils.TimeFormatter.formatDateHeader(voice.date)}", color = GhostTextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (fullScreenImageUrl != null) {
            FullScreenImageDialog(
                imageUrl = fullScreenImageUrl!!,
                onDismiss = { fullScreenImageUrl = null }
            )
        }

        // 💥 ДИАЛОГ 2: КОНТЕКСТНОЕ МЕНЮ ПО ДОЛГОМУ ТАПУ ("ПОКАЗАТЬ В ЧАТЕ")
        if (selectedMediaForMenu != null) {
            val media = selectedMediaForMenu!!
            AlertDialog(
                onDismissRequest = { selectedMediaForMenu = null },
                containerColor = GhostCard,
                title = { Text("Действия", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // 💥 ВОТ ОНА — КИЛЛЕР-ФИЧА: ПЕРЕХОДИМ К СООБЩЕНИЮ!
                                    onNavigateToMessage(media.id)
                                    selectedMediaForMenu = null
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Показать в чате", color = Color.White, fontSize = 15.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedMediaForMenu = null }) {
                        Text("Отмена", color = GhostTextSecondary)
                    }
                }
            )
        }
    }
}

// =============================================================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ДИЗАЙНА TELEGRAM
// =============================================================================

@Composable
private fun ProfileActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GhostCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = GhostTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileInfoItem(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = subtitle, color = GhostTextSecondary, fontSize = 12.sp)
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = GhostTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TelegramTabPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) GhostPrimary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) GhostPrimary else GhostTextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}