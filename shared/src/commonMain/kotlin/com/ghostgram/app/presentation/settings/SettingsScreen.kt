package com.ghostgram.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.presentation.components.avatar.SettingAvatar
import com.ghostgram.app.presentation.components.buttons.LogoutButton
import com.ghostgram.app.presentation.components.dialog.AiDialog
import com.ghostgram.app.presentation.components.dialog.CryptoDialog
import com.ghostgram.app.presentation.components.dialog.StorageDialog
import com.ghostgram.app.presentation.components.group.MultiAccountGroup
import com.ghostgram.app.presentation.components.group.PrivacyGroup
import com.ghostgram.app.presentation.components.group.SettingsGroup
import com.ghostgram.app.presentation.components.items.SettingsNavigationItem
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostBorder
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
        saveAiKey = { viewModel.saveCustomApiKey(it) },
        onClearNormalCache = {viewModel.toggleNormalCacheClear()},
        onClearAntiRevokeCache = {viewModel.toggleAntiRevokeCacheClear()},
        executeCacheClear = {viewModel.executeCacheClear()}
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
    onClearNormalCache: () -> Unit,
    onClearAntiRevokeCache: () -> Unit,
    executeCacheClear: () -> Unit,
    saveAiKey: (String) -> Unit,
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

            // 1. НЕОНОВЫЙ ХЕДЕР ПРОФИЛЯ (ИЗ СТИЧА!)
            item {
                SettingAvatar(
                    avatarPath = state.avatarPath,
                    userName = state.userName,
                    userHandle = state.userHandle,
                    phoneNumber = state.phoneNumber
                )
            }

            // 2. МУЛЬТИАККАУНТ-ХАБ (ТЕЛЕГРАМ-СТАЙЛ)
            item {
                MultiAccountGroup(
                    accounts = state.accounts,
                    currentAccountId = state.currentAccountId,
                    onSwitchAccount = onSwitchAccount,
                    onAddAccount = onAddAccount,
                )
            }

            // 3. СЕКЦИЯ GHOST ПРИВАТНОСТИ И КРИПТОГРАФИИ
            item {
                PrivacyGroup(
                    isStealthMode = state.isStealthMode,
                    onToggleStealthMode = onToggleStealthMode,
                    setCryptoDialogOpen = setCryptoDialogOpen
                )
            }

            // 4. ХРАНИЛИЩЕ И AI АССИСТЕНТ
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

            // 5. КНОПКА ВЫХОДА (ОПАСНАЯ ЗОНА)
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
                clearNormalCache = state.clearNormalCache,
                clearAntiRevokeCache = state.clearAntiRevokeCache,
                onClearNormalCache = onClearNormalCache,
                onClearAntiRevokeCache = onClearAntiRevokeCache,
                executeCacheClear = executeCacheClear
            )
        }
    }
}