package ca.lght.mindfulmail.di

import ca.lght.mindfulmail.data.repository.MailRepositoryImpl
import ca.lght.mindfulmail.domain.repository.MailRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMailRepository(impl: MailRepositoryImpl): MailRepository
}
