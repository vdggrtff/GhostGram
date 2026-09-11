package com.ghostgram.app.presentation.components.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.navigation.Screen
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurface
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun GhostBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // 💥 Внешний контейнер для позиционирования
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, bottom = 24.dp), // 💥 Идеальный синтаксис
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                // Мягкая фиолетовая тень под панелью
                .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = GhostPrimary, ambientColor = GhostPrimary)
                .clip(RoundedCornerShape(32.dp))
                // 💥 ЦВЕТ СТЕКЛА (Светлее фона, с альфа-каналом 60%)
                .background(Color(0xFF2A3040).copy(alpha = 0.6f))
                // 💥 БЛИК ПО КРАЯМ (Тонкая полупрозрачная белая рамка дает эффект объема!)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostBottomBarItem(
                icon = Icons.Default.ChatBubble,
                label = "Чаты",
                isSelected = currentRoute == Screen.ChatList.route,
                onClick = { onNavigate(Screen.ChatList.route) },
                modifier = Modifier.weight(1f)
            )
            GhostBottomBarItem(
                icon = Icons.Default.Group,
                label = "Контакты",
                isSelected = currentRoute == Screen.Contacts.route,
                onClick = { Screen.Contacts.route },
                modifier = Modifier.weight(1f)
            )
            GhostBottomBarItem(
                icon = Icons.Default.Settings,
                label = "Настройки",
                isSelected = currentRoute == Screen.Settings.route,
                onClick = { onNavigate(Screen.Settings.route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}



@Composable
private fun GhostBottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) GhostPrimary else GhostTextSecondary

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) GhostPrimary.copy(alpha = 0.15f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
    }
}

