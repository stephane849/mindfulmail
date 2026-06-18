package ca.lght.mindfulmail.di

import android.content.Context
import androidx.room.Room
import ca.lght.mindfulmail.data.local.db.ConversationDao
import ca.lght.mindfulmail.data.local.db.LabelDao
import ca.lght.mindfulmail.data.local.db.MailDatabase
import ca.lght.mindfulmail.data.local.db.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMailDatabase(@ApplicationContext context: Context): MailDatabase =
        Room.databaseBuilder(context, MailDatabase::class.java, "mindfulmail.db").build()

    @Provides
    fun provideMessageDao(db: MailDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideConversationDao(db: MailDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideLabelDao(db: MailDatabase): LabelDao = db.labelDao()
}
