package ca.lght.mindfulmail.di

import android.content.Context
import ca.lght.mindfulmail.data.remote.imap.ImapMailProvider
import ca.lght.mindfulmail.data.remote.proton.ProtonMailProvider
import ca.lght.mindfulmail.data.remote.proton.api.ProtonApiClient
import ca.lght.mindfulmail.data.remote.proton.auth.ProtonSessionStore
import ca.lght.mindfulmail.data.remote.proton.crypto.ProtonCryptoHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    @Provides
    @Singleton
    fun provideProtonSessionStore(
        @ApplicationContext context: Context,
    ): ProtonSessionStore = ProtonSessionStore(context)

    @Provides
    @Singleton
    fun provideProtonApiClient(
        httpClient: HttpClient,
        sessionStore: ProtonSessionStore,
    ): ProtonApiClient = ProtonApiClient(httpClient, sessionStore)

    @Provides
    @Singleton
    fun provideProtonCryptoHelper(): ProtonCryptoHelper = ProtonCryptoHelper()

    @Provides
    @Singleton
    fun provideProtonMailProvider(
        apiClient: ProtonApiClient,
        sessionStore: ProtonSessionStore,
        cryptoHelper: ProtonCryptoHelper,
    ): ProtonMailProvider = ProtonMailProvider(apiClient, sessionStore, cryptoHelper)

    @Provides
    @Singleton
    fun provideImapMailProvider(): ImapMailProvider = ImapMailProvider()
}
