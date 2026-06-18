package ca.lght.mindfulmail.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")

    object Inbox : Screen("inbox/{labelId}") {
        fun createRoute(labelId: String = "0") = "inbox/$labelId"
    }

    object MessageDetail : Screen("message/{messageId}") {
        fun createRoute(messageId: String) = "message/$messageId"
    }

    object Compose : Screen("compose?replyTo={replyTo}") {
        fun createRoute(replyTo: String? = null) =
            if (replyTo != null) "compose?replyTo=$replyTo" else "compose"
    }

    object Settings : Screen("settings")
}
