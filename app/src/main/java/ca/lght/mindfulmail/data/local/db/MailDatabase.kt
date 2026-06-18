package ca.lght.mindfulmail.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import ca.lght.mindfulmail.data.local.entity.ConversationEntity
import ca.lght.mindfulmail.data.local.entity.LabelEntity
import ca.lght.mindfulmail.data.local.entity.MessageEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        MessageEntity::class,
        ConversationEntity::class,
        LabelEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StringListConverter::class)
abstract class MailDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun labelDao(): LabelDao
}

class StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
