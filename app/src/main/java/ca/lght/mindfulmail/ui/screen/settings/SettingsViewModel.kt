package ca.lght.mindfulmail.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MailRepository,
) : ViewModel() {

    val account: StateFlow<Account?> = repository.activeAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
