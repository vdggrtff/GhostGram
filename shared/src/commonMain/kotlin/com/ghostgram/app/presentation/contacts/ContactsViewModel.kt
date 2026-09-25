package com.ghostgram.app.presentation.contacts

import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ContactRepository

class ContactsViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state: StateFlow<ContactsState> = _state.asStateFlow()

    private var contactsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    loadContacts(session.contactRepository)
                } else {
                    _state.update { it.copy(contacts = emptyList()) }
                }
            }
        }
    }

    private fun loadContacts(contactRepo: ContactRepository) {
        contactsJob?.cancel()
        _state.update { it.copy(isLoading = true) }

        contactsJob = viewModelScope.launch {
            contactRepo.observeContacts().collect { list ->
                _state.update { it.copy(isLoading = false, contacts = list) }
            }
        }
    }

    fun onIntent(intent: ContactsIntent, onNavigateToChat: (Long) -> Unit) {
        when (intent) {
            is ContactsIntent.OnSearchChanged -> _state.update { it.copy(searchQuery = intent.query) }
            is ContactsIntent.OnContactClick -> {
                viewModelScope.launch {
                    val repo = sessionManager.currentSession.value?.contactRepository ?: return@launch
                    val chatId = repo.createPrivateChat(intent.userId)
                    onNavigateToChat(chatId) // Мгновенно летим в диалог с этим человеком!
                }
            }
        }
    }
}