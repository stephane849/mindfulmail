package ca.lght.mindfulmail.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ca.lght.mindfulmail.ui.theme.Black
import ca.lght.mindfulmail.ui.theme.Gray
import ca.lght.mindfulmail.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsState()

    LaunchedEffect(account) {
        if (account == null) onLogout()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray)
            Spacer(Modifier.height(8.dp))
            Text(
                text = account?.email ?: "—",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = account?.displayName ?: "",
                fontSize = 16.sp,
                color = Gray,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.logout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Black, contentColor = White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Sign out")
            }
        }
    }
}
