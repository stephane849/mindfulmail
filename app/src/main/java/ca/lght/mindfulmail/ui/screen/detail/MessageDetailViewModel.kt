package ca.lght.mindfulmail.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val repository: MailRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle["messageId"])

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    init {
        viewModelScope.launch {
            repository.getMessage(messageId).onSuccess { _message.value = it }
            repository.markAsRead(listOf(messageId))
        }
    }
}
