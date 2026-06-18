package ca.lght.mindfulmail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.lght.mindfulmail.domain.model.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val subject: String,
    /** JSON-encoded EmailAddress */
    val senderJson: String,
    /** JSON-encoded List<EmailAddress> */
    val recipientsJson: String,
    val ccRecipientsJson: String,
    val bccRecipientsJson: String,
    val body: String,
    val bodyMimeType: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    /** JSON-encoded List<String> */
    val labelIds: String,
    /** JSON-encoded List<Attachment> */
    val attachmentsJson: String,
    val isEncrypted: Boolean,
)

fun MessageEntity.toMessage(): Message {
    val json = Json { ignoreUnknownKeys = true }
    return Message(
        id = id,
        conversationId = conversationId,
        subject = subject,
        sender = json.decodeFromString<EmailAddressDto>(senderJson).toDomain(),
        recipients = json.decodeFromString<List<EmailAddressDto>>(recipientsJson).map { it.toDomain() },
        ccRecipients = json.decodeFromString<List<EmailAddressDto>>(ccRecipientsJson).map { it.toDomain() },
        bccRecipients = json.decodeFromString<List<EmailAddressDto>>(bccRecipientsJson).map { it.toDomain() },
        body = body,
        bodyMimeType = bodyMimeType,
        timestamp = timestamp,
        isRead = isRead,
        isStarred = isStarred,
        labelIds = json.decodeFromString<List<String>>(labelIds),
        attachments = json.decodeFromString<List<AttachmentDto>>(attachmentsJson).map { it.toDomain() },
        isEncrypted = isEncrypted,
    )
}

fun Message.toEntity(): MessageEntity {
    val json = Json { ignoreUnknownKeys = true }
    return MessageEntity(
        id = id,
        conversationId = conversationId,
        subject = subject,
        senderJson = json.encodeToString(sender.toDto()),
        recipientsJson = json.encodeToString(recipients.map { it.toDto() }),
        ccRecipientsJson = json.encodeToString(ccRecipients.map { it.toDto() }),
        bccRecipientsJson = json.encodeToString(bccRecipients.map { it.toDto() }),
        body = body,
        bodyMimeType = bodyMimeType,
        timestamp = timestamp,
        isRead = isRead,
        isStarred = isStarred,
        labelIds = json.encodeToString(labelIds),
        attachmentsJson = json.encodeToString(attachments.map { it.toDto() }),
        isEncrypted = isEncrypted,
    )
}
