package com.ghostgram.app.presentation.components.bottombar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun GhostBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = GhostBackground,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "chat_list",
            onClick = { onNavigate("chat_list") },
            icon = { Icon(Filled.Chat, contentDescription = "Чаты") },
            label = { Text("Чаты") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GhostPrimary,
                unselectedIconColor = Color.Gray,
                indicatorColor = GhostCard // Темный фон при выделении
            )
        )

        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { onNavigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
            label = { Text("Настройки") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GhostPrimary,
                unselectedIconColor = Color.Gray,
                indicatorColor = GhostCard
            )
        )
    }
}