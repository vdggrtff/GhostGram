package com.ghostgram.app.presentation.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary

@Composable
fun FabGetDown(
    showScrollToBottom: Boolean,
    onClick: () -> Unit
){
    AnimatedVisibility(
        visible = showScrollToBottom,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier
            .padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = {
                onClick()
            },
            containerColor = GhostCard,
            contentColor = GhostPrimary,
            modifier = Modifier.size(44.dp),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Вниз",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}