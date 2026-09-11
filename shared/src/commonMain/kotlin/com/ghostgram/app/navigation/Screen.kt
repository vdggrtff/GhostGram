package com.ghostgram.app.navigation

sealed class Screen(val route: String) {

    object LoginScreen : Screen(LOGIN_SCREEN)
    object ChatList : Screen(CHAT_LIST_SCREEN)

    object Settings : Screen(SETTINGS_SCREEN)

    object Contacts : Screen(CONTACTS_SCREEN)

    companion object{
        const val CHAT_LIST_SCREEN = "chat_list"

        const val LOGIN_SCREEN = "login_screen"

        const val SETTINGS_SCREEN = "settings"

        const val CONTACTS_SCREEN = "contacts"
    }

}