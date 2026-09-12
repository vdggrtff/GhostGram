package com.ghostgram.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.components.avatar.SettingAvatar
import com.ghostgram.app.presentation.components.buttons.LogoutButton
import com.ghostgram.app.presentation.components.dialog.AiDialog
import com.ghostgram.app.presentation.components.dialog.CryptoDialog
import com.ghostgram.app.presentation.components.dialog.StorageDialog
import com.ghostgram.app.presentation.components.group.PrivacyGroup
import com.ghostgram.app.presentation.components.group.SettingsGroup
import com.ghostgram.app.presentation.components.items.SettingsNavigationItem
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.MyProfile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToAuth: () -> Unit,
    onNavigateToChatList: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    SettingsScreen(
        state = state,
        onToggleStealthMode = viewModel::onToggleStealthMode,
        onAddAccount = { viewModel.onAddAccount(onNavigateToAuth) },
        onSwitchAccount = { accountId ->
            viewModel.onSwitchAccount(
                accountId,
                onNavigateToChatList
            )
        },
        onLogOut = { viewModel.onLogOut(onNavigateToAuth, onNavigateToChatList) },
        setCryptoDialogOpen = { viewModel.setCryptoDialogOpen(it) },
        setAiDialogOpen = { viewModel.setAiDialogOpen(it) },
        setStorageDialogOpen = { viewModel.setStorageDialogOpen(it) },
        updateAiKeyInput = { viewModel.updateAiKeyInput(it) },
        onClear = { viewModel.clearCache() },
        saveAiKey = { viewModel.saveAiKey() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onToggleStealthMode: () -> Unit,
    onAddAccount: () -> Unit,
    setCryptoDialogOpen: (Boolean) -> Unit,
    setAiDialogOpen: (Boolean) -> Unit,
    setStorageDialogOpen: (Boolean) -> Unit,
    onSwitchAccount: (String) -> Unit,
    updateAiKeyInput: (String) -> Unit,
    onClear: () -> Unit,
    saveAiKey: () -> Unit,
    onLogOut: () -> Unit,
) {
    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GhostBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp), // Отступ под парящий BottomBar
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 💥 1. НЕОНОВЫЙ ХЕДЕР ПРОФИЛЯ (ИЗ СТИЧА!)
            item {
                SettingAvatar(
                    avatarPath = state.avatarPath,
                    userName = state.userName,
                    userHandle = state.userHandle,
                    phoneNumber = state.phoneNumber
                )
            }

            // 💥 2. МУЛЬТИАККАУНТ-ХАБ (ТЕЛЕГРАМ-СТАЙЛ)
            item {
                SettingsGroup(title = "Подключенные аккаунты") {
                    state.accounts.forEachIndexed { index, session ->
                        val isCurrent = session.accountId == state.currentAccountId

                        // 💥 МАГИЯ: Реактивно получаем профиль для КАЖДОГО аккаунта в списке!
                        val profile by session.chatRepository.observeMyProfile()
                            .collectAsState(initial = MyProfile())

                        // Если имя еще не загрузилось, показываем системный ID
                        val displayName =
                            if (profile.fullName.isNotBlank() && profile.fullName != "Ghost") profile.fullName else "Аккаунт ${index + 1}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchAccount(session.accountId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) GhostPrimary else GhostSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        displayName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        if (isCurrent) "Текущий профиль" else "Переключиться",
                                        color = GhostTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isCurrent) {
                                Box(
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                        .background(GhostPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = GhostPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < state.accounts.size - 1) {
                            HorizontalDivider(
                                color = GhostBorder.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 68.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = GhostBorder.copy(alpha = 0.5f))

                    // Кнопка добавления аккаунта
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddAccount() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(GhostPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = GhostPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            "Добавить аккаунт",
                            color = GhostPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // 💥 3. СЕКЦИЯ GHOST ПРИВАТНОСТИ И КРИПТОГРАФИИ
            item {
                PrivacyGroup(
                    isStealthMode = state.isStealthMode,
                    onToggleStealthMode = onToggleStealthMode,
                    setCryptoDialogOpen = setCryptoDialogOpen
                )
            }

            // 💥 4. ХРАНИЛИЩЕ И AI АССИСТЕНТ
            item {
                SettingsGroup(title = "Система и AI") {
                    SettingsNavigationItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Google Gemini AI",
                        subtitle = "Суммаризация чатов и умные ответы",
                        onClick = { setAiDialogOpen(true) }
                    )

                    HorizontalDivider(
                        color = GhostBorder.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Storage,
                        title = "Память и SQLite база",
                        subtitle = "Управление кэшем и файлами",
                        onClick = { setStorageDialogOpen(true) }
                    )

                    HorizontalDivider(
                        color = GhostBorder.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    SettingsNavigationItem(
                        icon = Icons.Default.Palette,
                        title = "Внешний вид и Тема",
                        subtitle = "Cyber-Ghost OLED Dark"
                    )
                }
            }

            // 💥 5. КНОПКА ВЫХОДА (ОПАСНАЯ ЗОНА)
            item {
                LogoutButton(
                    onLogOut = onLogOut
                )
            }
        }
        if (state.showAiDialog) {
            AiDialog(
                aiApiKeyInput = state.aiApiKeyInput,
                setAiDialogOpen = setAiDialogOpen,
                updateAiKeyInput = updateAiKeyInput,
                saveAiKey = saveAiKey
            )
        }

        // 2. ДИАЛОГ ИНФОРМАЦИИ О ШИФРОВАНИИ
        if (state.showCryptoDialog) {
            CryptoDialog(
                setCryptoDialogOpen = setCryptoDialogOpen
            )
        }

        // 3. ДИАЛОГ ОЧИСТКИ ПАМЯТИ
        if (state.showStorageDialog) {
            StorageDialog(
                setStorageDialogOpen = setStorageDialogOpen,
                onClear = onClear
            )
        }
    }
}