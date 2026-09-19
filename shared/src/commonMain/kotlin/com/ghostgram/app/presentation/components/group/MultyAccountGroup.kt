package com.ghostgram.app.presentation.components.group

import AccountSession
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.MyProfile

@Composable
fun MultiAccountGroup(
    accounts: List<AccountSession>,
    currentAccountId: String?,
    onSwitchAccount: (String) -> Unit,
    onAddAccount: () -> Unit
){
    SettingsGroup(title = "Подключенные аккаунты") {
        accounts.forEachIndexed { index, session ->
            val isCurrent = session.accountId == currentAccountId

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

            if (index < accounts.size - 1) {
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