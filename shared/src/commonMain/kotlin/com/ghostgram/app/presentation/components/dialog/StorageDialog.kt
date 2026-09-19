package com.ghostgram.app.presentation.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Checkbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghostgram.app.ui.theme.GhostAccentRed
import com.ghostgram.app.ui.theme.GhostBorder
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary

@Composable
fun StorageDialog(
    setStorageDialogOpen: (Boolean) -> Unit,
    clearNormalCache: Boolean,
    clearAntiRevokeCache: Boolean,
    onClearNormalCache: () -> Unit,
    onClearAntiRevokeCache: () -> Unit,
    executeCacheClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { setStorageDialogOpen(false) },
        containerColor = GhostCard,
        title = { Text("Управление памятью", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // --- СЕКЦИЯ: РУЧНАЯ ОЧИСТКА ---
                Text(
                    "РУЧНАЯ ОЧИСТКА",
                    color = GhostPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Чекбокс 1: Обычный кэш
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onClearNormalCache() }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearNormalCache,
                        onCheckedChange = { onClearNormalCache() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GhostPrimary,
                            uncheckedColor = GhostTextSecondary
                        )
                    )
                    Column {
                        Text("Обычный кэш", color = Color.White, fontSize = 15.sp)
                        Text(
                            "Медиа и обычные сообщения",
                            color = GhostTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Чекбокс 2: Anti-Revoke
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onClearAntiRevokeCache() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = clearAntiRevokeCache,
                        onCheckedChange = { onClearAntiRevokeCache() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GhostAccentRed,
                            uncheckedColor = GhostTextSecondary
                        )
                    )
                    Column {
                        Text(
                            "Anti-Revoke архив",
                            color = GhostAccentRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Сохраненные удаленные сообщения",
                            color = GhostTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GhostBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // --- СЕКЦИЯ: АВТООЧИСТКА (Визуальный задел на будущее) ---
                Text(
                    "АВТОМАТИЧЕСКАЯ ОЧИСТКА",
                    color = GhostPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Обычный кэш", color = Color.White, fontSize = 14.sp)
                    Text(
                        "1 месяц ▾",
                        color = GhostPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ) // TODO: Сделать DropdownMenu
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anti-Revoke архив", color = Color.White, fontSize = 14.sp)
                    Text(
                        "Никогда ▾",
                        color = GhostPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ) // TODO: Сделать DropdownMenu
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { executeCacheClear() },
                // Кнопка активна только если выбран хотя бы один чекбокс!
                enabled = clearNormalCache || clearAntiRevokeCache
            ) {
                Text(
                    text = "Очистить",
                    color = if (clearNormalCache || clearAntiRevokeCache) GhostAccentRed else GhostTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { setStorageDialogOpen(false) }) {
                Text("Отмена", color = GhostTextSecondary)
            }
        }
    )
}
/*AlertDialog(
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
)*/