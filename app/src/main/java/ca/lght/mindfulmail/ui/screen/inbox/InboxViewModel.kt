package ca.lght.mindfulmail.ui.screen.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: MailRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val labelId: String = savedStateHandle["labelId"] ?: "0"

    val conversations: StateFlow<List<Conversation>> = repository
        .getConversations(labelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch { repository.sync() }
    }
}
