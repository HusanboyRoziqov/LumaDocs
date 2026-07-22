package app.lumadocs.kmp.data_store

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lumadocs.kmp.LumaDocsApplication

@Composable
actual fun rememberDataStore(): DataStore<Preferences> {
    return LumaDocsApplication.dataStore
}
