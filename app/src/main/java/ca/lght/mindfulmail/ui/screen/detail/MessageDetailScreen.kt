package ca.lght.mindfulmail.ui.screen.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ca.lght.mindfulmail.ui.theme.Gray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    onBack: () -> Unit,
    onReply: (messageId: String) -> Unit,
    viewModel: MessageDetailViewModel = hiltViewModel(),
) {
    val message by viewModel.message.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(message?.subject ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            message?.let { msg ->
                FloatingActionButton(onClick = { onReply(msg.id) }) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply")
                }
            }
        },
    ) { padding ->
        message?.let { msg ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(msg.subject, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text("From: ${msg.sender}", color = Gray)
                Text(
                    "To: ${msg.recipients.joinToString()}",
                    color = Gray,
                )
                if (msg.ccRecipients.isNotEmpty()) {
                    Text("CC: ${msg.ccRecipients.joinToString()}", color = Gray)
                }
                Text(
                    text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                        .format(Date(msg.timestamp)),
                    color = Gray,
                )
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                SelectionContainer {
                    Text(
                        text = msg.body.stripHtml(),
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                    )
                }
            }
        }
    }
}

private fun String.stripHtml(): String =
    replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace(Regex("&amp;"), "&")
        .replace(Regex("&lt;"), "<")
        .replace(Regex("&gt;"), ">")
        .replace(Regex("&nbsp;"), " ")
        .replace(Regex("&quot;"), "\"")
        .trim()
