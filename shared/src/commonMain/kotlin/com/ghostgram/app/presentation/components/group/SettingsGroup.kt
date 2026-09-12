package com.ghostgram.app.presentation.components.group

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostShapes
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = GhostTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = GhostCard),
            shape = GhostShapes.medium,
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, GhostBorder.copy(alpha = 0.5f), GhostShapes.medium)
        ) {
            content()
        }
    }
}