package ca.lght.mindfulmail.domain.model

data class Conversation(
    val id: String,
    val subject: String,
    val senders: List<EmailAddress>,
    val numMessages: Int,
    val numUnread: Int,
    val latestTimestamp: Long,
    val labelIds: List<String> = emptyList(),
    val isStarred: Boolean = false,
)
