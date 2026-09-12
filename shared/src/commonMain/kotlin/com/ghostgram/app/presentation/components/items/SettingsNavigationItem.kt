package com.ghostgram.app.presentation.components.items

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecondary
import com.ghostgram.app.ui.theme.GhostTextMuted
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = GhostPrimary,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = GhostSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = GhostTextSecondary, fontSize = 12.sp)
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = GhostTextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}