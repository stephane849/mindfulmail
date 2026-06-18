package ca.lght.mindfulmail.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.lght.mindfulmail.data.local.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY name ASC")
    fun getAll(): Flow<List<LabelEntity>>

    @Upsert
    suspend fun upsert(labels: List<LabelEntity>)
}
