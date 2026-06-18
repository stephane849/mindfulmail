package ca.lght.mindfulmail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ca.lght.mindfulmail.ui.navigation.MindfulMailNavGraph
import ca.lght.mindfulmail.ui.theme.MindfulMailTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable window animations for E Ink display — animations cause ghosting
        window.setWindowAnimations(0)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        enableEdgeToEdge()

        setContent {
            MindfulMailTheme {
                MindfulMailNavGraph()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
