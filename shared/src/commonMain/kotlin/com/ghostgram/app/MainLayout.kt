package com.ghostgram.app

import SessionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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

    val showBottomBar = currentRoute == Screen.ChatList.route || currentRoute == Screen.Settings.route

    val sessionManager: SessionManager = koinInject()
    val currentSession by sessionManager.currentSession.collectAsState()

    LaunchedEffect(Unit) {
        sessionManager.initDefaultAccount() // 💥 Всегда один и тот же "main_account"!
    }

    if (currentSession == null) {
        Box(modifier = Modifier.fillMaxSize().background(GhostBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GhostPrimary)
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GhostBackground,
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
                .padding(top = paddingValues.calculateTopPadding())
                .background(GhostBackground)
        ) {
            AppNavGraph(navController)
        }
    }
}