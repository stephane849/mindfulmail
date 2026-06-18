package ca.lght.mindfulmail.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.lght.mindfulmail.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMessagesInConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE messages SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<String>)
}
