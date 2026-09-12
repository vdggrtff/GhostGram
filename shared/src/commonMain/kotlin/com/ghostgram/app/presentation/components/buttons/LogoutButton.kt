package com.ghostgram.app.presentation.components.buttons

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostShapes

@Composable
fun LogoutButton(
    onLogOut: () -> Unit
){
    Card(
        colors = CardDefaults.cardColors(containerColor = GhostCard),
        shape = GhostShapes.medium,
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, GhostAccentRed.copy(alpha = 0.2f), GhostShapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogOut() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                tint = GhostAccentRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "Выйти из текущего аккаунта",
                color = GhostAccentRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}