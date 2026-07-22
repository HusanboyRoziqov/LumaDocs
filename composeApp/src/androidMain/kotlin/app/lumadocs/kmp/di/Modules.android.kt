package app.lumadocs.kmp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.GoogleAuthenticator
import app.lumadocs.kmp.LumaDocsApplication
import app.lumadocs.kmp.data_store.createDataStore
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.services.GoogleDriveRepositoryImpl
import app.lumadocs.kmp.viewmodels.AddScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<Authenticator> { GoogleAuthenticator(androidContext()) }
    single<GoogleDriveRepository> { GoogleDriveRepositoryImpl(androidContext()) }
    single<DataStore<Preferences>> { LumaDocsApplication.dataStore }
    factory<AddScreenViewModel> { AddScreenViewModel() }
}
