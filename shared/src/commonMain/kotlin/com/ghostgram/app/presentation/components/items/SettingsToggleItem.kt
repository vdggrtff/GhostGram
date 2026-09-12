package com.ghostgram.app.presentation.components.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSurfaceElevated
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = GhostPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = GhostTextSecondary, fontSize = 12.sp)
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GhostPrimary,
                uncheckedThumbColor = GhostTextSecondary,
                uncheckedTrackColor = GhostSurfaceElevated
            )
        )
    }
}