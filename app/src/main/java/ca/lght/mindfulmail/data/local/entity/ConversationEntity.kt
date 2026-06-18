package ca.lght.mindfulmail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.lght.mindfulmail.domain.model.Conversation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val subject: String,
    /** JSON-encoded List<EmailAddressDto> */
    val sendersJson: String,
    val numMessages: Int,
    val numUnread: Int,
    val latestTimestamp: Long,
    /** JSON-encoded List<String> — queried with LIKE pattern */
    val labelIds: String,
    val isStarred: Boolean,
)

fun ConversationEntity.toConversation(): Conversation {
    val json = Json { ignoreUnknownKeys = true }
    return Conversation(
        id = id,
        subject = subject,
        senders = json.decodeFromString<List<EmailAddressDto>>(sendersJson).map { it.toDomain() },
        numMessages = numMessages,
        numUnread = numUnread,
        latestTimestamp = latestTimestamp,
        labelIds = json.decodeFromString<List<String>>(labelIds),
        isStarred = isStarred,
    )
}

fun Conversation.toEntity(): ConversationEntity {
    val json = Json { ignoreUnknownKeys = true }
    return ConversationEntity(
        id = id,
        subject = subject,
        sendersJson = json.encodeToString(senders.map { EmailAddressDto(it.name, it.address) }),
        numMessages = numMessages,
        numUnread = numUnread,
        latestTimestamp = latestTimestamp,
        labelIds = json.encodeToString(labelIds),
        isStarred = isStarred,
    )
}
