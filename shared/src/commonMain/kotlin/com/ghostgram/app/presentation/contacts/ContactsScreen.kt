package com.ghostgram.app.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghostgram.app.presentation.components.input.GhostSearchBar
import com.ghostgram.app.ui.theme.GhostAccentGreen
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostPrimary
import com.ghostgram.app.ui.theme.GhostTextSecondary
import entity.Contact
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsRoute(
    viewModel: ContactsViewModel = koinViewModel(),
    onNavigateToChat: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()

    ContactsScreen(
        state = state,
        onIntent = { intent -> viewModel.onIntent(intent, onNavigateToChat) }
    )
}

@Composable
fun ContactsScreen(
    state: ContactsState,
    onIntent: (ContactsIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(GhostBackground)
    ) {
        // Шапка с заголовком и тонким поиском
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Контакты",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            GhostSearchBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(ContactsIntent.OnSearchChanged(it)) }
            )
        }

        if (state.isLoading && state.contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GhostPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp) // Отступ под парящий BottomBar
            ) {
                // Итерируемся по буквам алфавита ('А', 'Б', 'В'...)
                state.groupedContacts.forEach { (letter, contactsInGroup) ->
                    // 💥 Заголовок секции (Буква алфавита)
                    item(key = "header_$letter") {
                        Text(
                            text = letter.toString(),
                            color = GhostPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    // Список людей под этой буквой
                    items(contactsInGroup.size, key = { contactsInGroup[it].userId }) { index ->
                        val contact = contactsInGroup[index]
                        GhostContactItem(
                            contact = contact,
                            onClick = { onIntent(ContactsIntent.OnContactClick(contact.userId)) }
                        )
                    }
                }
            }
        }
    }
}

// 💥 Компонент строки контакта
@Composable
fun GhostContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка с индикатором онлайна
        Box(contentAlignment = Alignment.BottomEnd) {
            if (contact.avatarPath != null) {
                AsyncImage(
                    model = contact.avatarPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GhostPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.fullName.take(1).uppercase(),
                        color = GhostPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Зеленая точка онлайна
            if (contact.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(GhostBackground)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(GhostAccentGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.fullName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (contact.isOnline) "в сети" else contact.statusText,
                color = if (contact.isOnline) GhostAccentGreen else GhostTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}