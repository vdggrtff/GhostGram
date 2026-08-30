package com.ghostgram.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ghostgram.app.presentation.chats.ChatListRoute
import com.ghostgram.app.presentation.chats.ChatListScreen
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsRoute

sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object ChatDetails : Screen("chat_details/{chatId}")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.ChatList.route
    ) {
        // Экран списка чатов
        composable("chat_list") {
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
    }
}