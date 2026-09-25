package com.ghostgram.app.presentation.contacts

import entity.Contact

data class ContactsState(
    val contacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    // Отфильтрованные и сгруппированные по буквам контакты
    val groupedContacts: Map<Char, List<Contact>>
        get() {
            val filtered = if (searchQuery.isBlank()) contacts else {
                contacts.filter {
                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                            it.username?.contains(searchQuery, ignoreCase = true) == true ||
                            it.phoneNumber.contains(searchQuery)
                }
            }
            return filtered.sortedBy { it.fullName }.groupBy { it.initialLetter }
        }
}

sealed interface ContactsIntent {
    data class OnSearchChanged(val query: String) : ContactsIntent
    data class OnContactClick(val userId: Long) : ContactsIntent
}