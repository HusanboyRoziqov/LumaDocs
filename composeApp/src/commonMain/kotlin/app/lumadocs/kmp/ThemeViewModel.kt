package app.lumadocs.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ThemeViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val darkModeKey = booleanPreferencesKey("darkMode")

    // Read the saved value once, synchronously, so the very first frame uses the correct
    // theme (avoids a dark → light flash on startup).
    private val initialDarkMode: Boolean =
        runCatching { runBlocking { dataStore.data.first()[darkModeKey] } }.getOrNull() ?: true

    val isDarkMode = dataStore
        .data
        .map { prefs -> prefs[darkModeKey] ?: true } // default: dark
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            initialDarkMode
        )

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[darkModeKey] = dark }
        }
    }
}
