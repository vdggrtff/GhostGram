package com.ghostgram.app.presentation.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostPrimaryGradient
import com.ghostgram.app.ui.theme.GhostSurface
import com.ghostgram.app.ui.theme.GhostTextSecondary


@Composable
fun SettingAvatar(
    avatarPath: String?,
    userName: String,
    userHandle: String,
    phoneNumber: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        // Аватар с градиентным неоновым свечением
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    24.dp,
                    CircleShape,
                    spotColor = GhostPrimary,
                    ambientColor = GhostPrimary
                )
                .clip(CircleShape)
                .background(GhostPrimaryGradient)
                .padding(3.dp)
                .clip(CircleShape)
                .background(GhostSurface),
            contentAlignment = Alignment.Center
        ) {
            if (avatarPath != null && !avatarPath.startsWith("INITIALS:")) {
                AsyncImage(
                    model = if (avatarPath.startsWith("/")) "file://${avatarPath}" else avatarPath,
                    contentDescription = "Аватар",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = userName.take(1).uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userName,
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = userHandle,
            fontSize = 15.sp,
            color = GhostPrimary, // Неоновый юзернейм
            fontWeight = FontWeight.Medium
        )
        /*Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = phoneNumber,
            fontSize = 13.sp,
            color = GhostTextSecondary
        )*/
    }
}
