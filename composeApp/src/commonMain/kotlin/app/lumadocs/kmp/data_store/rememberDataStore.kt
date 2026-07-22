package app.lumadocs.kmp.data_store

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

@Composable
expect fun rememberDataStore(): DataStore<Preferences>
