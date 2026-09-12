package com.ghostgram.app.presentation.components.group

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgram.app.presentation.components.items.SettingsNavigationItem
import com.ghostgram.app.presentation.components.items.SettingsToggleItem
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun PrivacyGroup(
    isStealthMode: Boolean,
    onToggleStealthMode: () -> Unit,
    setCryptoDialogOpen: (Boolean) -> Unit,
){
    SettingsGroup(title = "Ghost Безопасность") {
        // Тумблер Stealth Mode
        SettingsToggleItem(
            icon = Icons.Default.VisibilityOff,
            title = "Stealth Mode (Невидимка)",
            subtitle = if (isStealthMode) "Онлайн и прочтения скрыты" else "Обычный режим",
            isChecked = isStealthMode,
            onCheckedChange = { onToggleStealthMode() }
        )

        HorizontalDivider(
            color = GhostBorder.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 54.dp)
        )

        // Статус Anti-Revoke
        SettingsNavigationItem(
            icon = Icons.Default.Security,
            title = "Anti-Revoke Shield",
            subtitle = "Активен • Сохранение удаленных сообщений",
            badge = "ON",
            badgeColor = GhostAccentGreen
        )

        HorizontalDivider(
            color = GhostBorder.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 54.dp)
        )

        // Статус CryptoLayer
        SettingsNavigationItem(
            icon = Icons.Default.Lock,
            title = "CryptoLayer E2EE",
            subtitle = "ECDH SECP256R1 + AES-256-GCM",
            badge = "INFO",
            badgeColor = GhostPrimary,
            onClick = { setCryptoDialogOpen(true) }
        )
    }
}