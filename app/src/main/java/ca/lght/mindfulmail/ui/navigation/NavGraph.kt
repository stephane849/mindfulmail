package ca.lght.mindfulmail.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.lght.mindfulmail.ui.screen.auth.LoginScreen
import ca.lght.mindfulmail.ui.screen.compose.ComposeScreen
import ca.lght.mindfulmail.ui.screen.detail.MessageDetailScreen
import ca.lght.mindfulmail.ui.screen.inbox.InboxScreen
import ca.lght.mindfulmail.ui.screen.settings.SettingsScreen

@Composable
fun MindfulMailNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Inbox.createRoute()) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Inbox.route,
            arguments = listOf(navArgument("labelId") { type = NavType.StringType }),
        ) {
            InboxScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.MessageDetail.createRoute(conversationId))
                },
                onComposeClick = {
                    navController.navigate(Screen.Compose.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(
            route = Screen.MessageDetail.route,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType }),
        ) {
            MessageDetailScreen(
                onBack = { navController.popBackStack() },
                onReply = { messageId ->
                    navController.navigate(Screen.Compose.createRoute(replyTo = messageId))
                },
            )
        }

        composable(
            route = Screen.Compose.route,
            arguments = listOf(
                navArgument("replyTo") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ComposeScreen(
                onBack = { navController.popBackStack() },
                onSent = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
