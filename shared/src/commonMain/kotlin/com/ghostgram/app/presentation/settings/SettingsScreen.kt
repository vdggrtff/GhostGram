package com.ghostgram.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToAuth: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    SettingsScreen(
        state = state,
        onAddAccount = { viewModel.onAddAccount(onNavigateToAuth) },
        onSwitchAccount = viewModel::onSwitchAccount,
        onLogOut = { viewModel.onLogOut(onNavigateToAuth) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAddAccount: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onLogOut: () -> Unit
) {
    Scaffold(
        containerColor = GhostBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GhostBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватарка текущего профиля
            item {
                Box(
                    modifier = Modifier.size(90.dp).clip(CircleShape).background(GhostPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👻", fontSize = 40.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.currentAccountId ?: "Аккаунт",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // 💥 СПИСОК ВСЕХ АККАУНТОВ (МУЛЬТИАККАУНТ!)
            item {
                Text(
                    text = "Подключенные аккаунты",
                    color = GhostTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = GhostCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        state.accounts.forEach { session ->
                            val isCurrent = session.accountId == state.currentAccountId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitchAccount(session.accountId) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isCurrent) GhostPrimary else GhostBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(session.accountId.takeLast(2), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(session.accountId, color = Color.White, fontSize = 16.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                }

                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GhostPrimary)
                                }
                            }
                            HorizontalDivider(color = GhostBackground)
                        }

                        // 💥 КНОПКА ДОБАВЛЕНИЯ ВТОРОГО АККАУНТА
                        TextButton(
                            onClick = onAddAccount,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GhostPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Добавить аккаунт", color = GhostPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Кнопка выхода
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GhostCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onLogOut,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Выйти из текущего аккаунта", color = Color(0xFFFF5252), fontSize = 16.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}