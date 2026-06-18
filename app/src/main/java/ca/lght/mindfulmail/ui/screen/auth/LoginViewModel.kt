package ca.lght.mindfulmail.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.lght.mindfulmail.data.remote.imap.ImapMailProvider
import ca.lght.mindfulmail.data.remote.proton.ProtonMailProvider
import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val account: Account) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: MailRepository,
    private val protonProvider: ProtonMailProvider,
    private val imapProvider: ImapMailProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun loginWithProton(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.setActiveProvider(protonProvider)
            repository.login(MailCredentials.ProtonCredentials(username, password))
                .fold(
                    onSuccess = { _uiState.value = LoginUiState.Success(it) },
                    onFailure = { _uiState.value = LoginUiState.Error(it.message ?: "Login failed") },
                )
        }
    }

    fun loginWithImap(
        email: String,
        password: String,
        imapHost: String,
        imapPort: Int,
        smtpHost: String,
        smtpPort: Int,
    ) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.setActiveProvider(imapProvider)
            repository.login(
                MailCredentials.ImapCredentials(
                    email = email,
                    password = password,
                    imapHost = imapHost,
                    imapPort = imapPort,
                    smtpHost = smtpHost,
                    smtpPort = smtpPort,
                ),
            ).fold(
                onSuccess = { _uiState.value = LoginUiState.Success(it) },
                onFailure = { _uiState.value = LoginUiState.Error(it.message ?: "Login failed") },
            )
        }
    }

    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}
