package app.lumadocs.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LanguageViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val languageCodeKey = stringPreferencesKey("languageCode")

    // Russian is the default the first time the app is opened; once the user picks a
    // language it is persisted and used instead.
    private val defaultLanguage = "ru"

    // Read the saved value once, synchronously, so the first frame uses the right language.
    private val initialLanguage: String =
        runCatching { runBlocking { dataStore.data.first()[languageCodeKey] } }.getOrNull()
            ?: defaultLanguage

    val languageCode = dataStore
        .data
        .map { prefs -> prefs[languageCodeKey] ?: defaultLanguage }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            initialLanguage
        )

    fun switchLanguage(languageCode: String) {
        viewModelScope.launch {
            dataStore.edit { mutablePrefs ->
                mutablePrefs[languageCodeKey] = languageCode
            }
        }
    }
}
