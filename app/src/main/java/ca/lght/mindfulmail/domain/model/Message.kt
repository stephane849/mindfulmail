package ca.lght.mindfulmail.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val subject: String,
    val sender: EmailAddress,
    val recipients: List<EmailAddress>,
    val ccRecipients: List<EmailAddress> = emptyList(),
    val bccRecipients: List<EmailAddress> = emptyList(),
    val body: String,
    val bodyMimeType: String = "text/plain",
    val timestamp: Long,
    val isRead: Boolean,
    val isStarred: Boolean = false,
    val labelIds: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val isEncrypted: Boolean = false,
)

data class EmailAddress(val name: String?, val address: String) {
    override fun toString(): String = if (name != null) "$name <$address>" else address
}
