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
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ca.lght.mindfulmail.ui.theme.Black
import ca.lght.mindfulmail.ui.theme.Gray
import ca.lght.mindfulmail.ui.theme.White

private val syncIntervals = listOf(
    "15 minutes" to 15,
    "30 minutes" to 30,
    "1 hour" to 60,
)

// TODO: replace with MMD equivalent when API is confirmed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val account by viewModel.account.collectAsState()
    var syncIntervalExpanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf(syncIntervals[0]) }

    LaunchedEffect(account) {
        // Navigate to login when account is cleared after logout
    }

    Scaffold(
        topBar = {
            // TODO: replace with MMD equivalent when API is confirmed
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

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text("Sync interval", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Gray)
            Spacer(Modifier.height(8.dp))

            // TODO: replace with MMD equivalent when API is confirmed
            ExposedDropdownMenuBox(
                expanded = syncIntervalExpanded,
                onExpandedChange = { syncIntervalExpanded = !syncIntervalExpanded },
            ) {
                OutlinedTextField(
                    value = selectedInterval.first,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = syncIntervalExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = syncIntervalExpanded,
                    onDismissRequest = { syncIntervalExpanded = false },
                ) {
                    syncIntervals.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval.first) },
                            onClick = {
                                selectedInterval = interval
                                syncIntervalExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Divider()
            Spacer(Modifier.height(32.dp))

            // TODO: replace with MMD equivalent when API is confirmed
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Black, contentColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Sign out")
            }
        }
    }
}
