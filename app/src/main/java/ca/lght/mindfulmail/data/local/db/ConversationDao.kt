package ca.lght.mindfulmail.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.lght.mindfulmail.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE labelIds LIKE '%\"' || :labelId || '\"%' ORDER BY latestTimestamp DESC")
    fun getByLabel(labelId: String): Flow<List<ConversationEntity>>

    @Upsert
    suspend fun upsert(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
