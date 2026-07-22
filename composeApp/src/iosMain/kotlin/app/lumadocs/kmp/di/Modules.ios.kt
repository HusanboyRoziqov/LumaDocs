package app.lumadocs.kmp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.IosGoogleAuthenticator
import app.lumadocs.kmp.data_store.createDataStore
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.services.IosGoogleDriveRepositoryImpl
import org.koin.dsl.module

actual val platformModule = module {
    // One instance behind both roles: it owns the GIDSignIn session, so it is also what
    // vends Drive access tokens to the repository.
    single { IosGoogleAuthenticator() }
    single<Authenticator> { get<IosGoogleAuthenticator>() }
    single<GoogleDriveRepository> { IosGoogleDriveRepositoryImpl(get<IosGoogleAuthenticator>()) }
    single<DataStore<Preferences>> { createDataStore() }
}
