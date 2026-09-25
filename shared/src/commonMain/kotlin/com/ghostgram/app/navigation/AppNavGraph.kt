package com.ghostgram.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ghostgram.app.presentation.auth.AuthRoute
import com.ghostgram.app.presentation.chats.ChatListRoute
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsRoute
import com.ghostgram.app.presentation.contacts.ContactsRoute
import com.ghostgram.app.presentation.settings.SettingsRoute

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.LoginScreen.route
    ) {

        composable(Screen.LoginScreen.route) {
            AuthRoute(
                onAuthSuccess = {
                    // Переходим на чаты и удаляем экран авторизации из бэкстека
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.LoginScreen.route) { inclusive = true }
                    }
                },
                onNavigateBack = { // ВОЗВРАТ В НАСТРОЙКИ
                    navController.popBackStack()
                }
            )
        }

        // Экран списка чатов
        composable(Screen.ChatList.route) {
            ChatListRoute(
                onNavigateToChat = { chatId ->
                    navController.navigate("chat_details/$chatId")
                }
            )
        }

        composable(
            route = "chat_details/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            ChatDetailsRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Contacts.route) {
            ContactsRoute(
                onNavigateToChat = { chatId ->
                    navController.navigate("chat_details/$chatId")
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsRoute(
                onNavigateToAuth = { navController.navigate(Screen.LoginScreen.route){
                    popUpTo(navController.graph.id) { inclusive = true }
                } },
                onNavigateToChatList = { // ДОБАВИЛИ МАРШРУТ
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                })
        }
    }
}