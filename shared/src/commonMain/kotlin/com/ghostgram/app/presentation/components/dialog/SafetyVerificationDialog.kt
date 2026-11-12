package com.ghostgram.app.presentation.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostCard
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostSecureGreen
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.SafetyFingerprint

@Composable
fun SafetyVerificationDialog(
    chatTitle: String,
    fingerprint: SafetyFingerprint?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhostCard,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Шифрование сквозное (E2EE)",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Сравните эти 4 эмодзи с собеседником при звонке или встрече. Если они совпадают, прослушивание невозможно.",
                    color = GhostTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 💥 4 ЭМОДЗИ БЕЗОПАСНОСТИ (FINGERPRINT):
                if (fingerprint != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2430))
                            .border(1.dp, GhostSecureGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        fingerprint.emojis.forEach { emoji ->
                            Text(text = emoji, fontSize = 34.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 64-значный шестнадцатеричный отпечаток
                    Text(
                        text = fingerprint.formattedHex,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "🔒 Ключи шифрования еще не согласованы. Нажмите на замок для отправки запроса.",
                        color = GhostTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(GhostSecureGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Защита от Man-in-the-Middle (MITM) активна", color = GhostSecureGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GhostPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Понятно", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}