package ca.lght.mindfulmail.ui.screen.compose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.lght.mindfulmail.domain.model.EmailAddress
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ComposeUiState {
    object Idle : ComposeUiState()
    object Sending : ComposeUiState()
    object Sent : ComposeUiState()
    data class Error(val message: String) : ComposeUiState()
}

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val repository: MailRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val replyToMessageId: String? = savedStateHandle["replyTo"]

    val to = MutableStateFlow("")
    val cc = MutableStateFlow("")
    val bcc = MutableStateFlow("")
    val subject = MutableStateFlow("")
    val body = MutableStateFlow("")

    private val _uiState = MutableStateFlow<ComposeUiState>(ComposeUiState.Idle)
    val uiState: StateFlow<ComposeUiState> = _uiState

    fun send() {
        viewModelScope.launch {
            _uiState.value = ComposeUiState.Sending
            val draft = MessageDraft(
                to = parseAddresses(to.value),
                cc = parseAddresses(cc.value),
                bcc = parseAddresses(bcc.value),
                subject = subject.value,
                body = body.value,
                replyToMessageId = replyToMessageId,
            )
            repository.sendMessage(draft).fold(
                onSuccess = { _uiState.value = ComposeUiState.Sent },
                onFailure = { _uiState.value = ComposeUiState.Error(it.message ?: "Send failed") },
            )
        }
    }

    private fun parseAddresses(raw: String): List<EmailAddress> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { EmailAddress(null, it) }
}
