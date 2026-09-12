package com.ghostgram.app.presentation.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun StorageDialog(
    setStorageDialogOpen: (Boolean) -> Unit,
    onClear: () -> Unit
){
    AlertDialog(
        onDismissRequest = { setStorageDialogOpen(false) },
        containerColor = GhostCard,
        title = { Text("Очистка кэша", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Вы уверены, что хотите удалить загруженные фото, видео и локальный кэш базы данных? " +
                        "(Сообщения Anti-Revoke не будут затронуты).",
                color = GhostTextSecondary, fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = { onClear() }) {
                Text("Очистить", color = GhostAccentRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { setStorageDialogOpen(false) }) {
                Text("Отмена", color = GhostTextSecondary)
            }
        }
    )
}