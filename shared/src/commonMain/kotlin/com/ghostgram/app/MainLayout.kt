package com.ghostgram.app

import SessionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ghostgram.app.navigation.AppNavGraph
import com.ghostgram.app.navigation.Screen
import com.ghostgram.app.presentation.components.bottombar.GhostBottomBar
import com.ghostgram.app.ui.theme.GhostBackground
import com.ghostgram.app.ui.theme.GhostPrimary
import org.koin.compose.koinInject

@Composable
fun MainLayout() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == Screen.ChatList.route || currentRoute == Screen.Settings.route || currentRoute == Screen.Contacts.route

    val sessionManager: SessionManager = koinInject()
    val currentSession by sessionManager.currentSession.collectAsState()

    LaunchedEffect(Unit) {
        // Грузим аккаунты с диска!
        sessionManager.loadSavedAccounts()
    }

    if (currentSession == null) {
        Box(modifier = Modifier.fillMaxSize().background(GhostBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GhostPrimary)
        }
        return
    }

    Scaffold(
        containerColor = GhostBackground,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        bottomBar = {
            if (showBottomBar) {
                GhostBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GhostBackground)
        ) {
            AppNavGraph(navController)
        }
    }
}