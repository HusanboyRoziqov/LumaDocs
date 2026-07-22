package app.lumadocs.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Persisted toggle for the connection/refresh banner on the Documents screen. */
class BannerSettingsViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val bannerKey = booleanPreferencesKey("bannerEnabled")

    val bannerEnabled = dataStore
        .data
        .map { prefs -> prefs[bannerKey] ?: true } // default: on
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            true
        )

    fun setBannerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[bannerKey] = enabled }
        }
    }

    private val viewModeKey = stringPreferencesKey("documentsViewMode")

    /** Persisted list/grid layout for the Documents screen. Stores the [DocumentsViewMode] name. */
    val viewMode = dataStore
        .data
        .map { prefs -> prefs[viewModeKey] ?: DocumentsViewMode.LIST.name } // default: list
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            DocumentsViewMode.LIST.name
        )

    fun setViewMode(mode: DocumentsViewMode) {
        viewModelScope.launch {
            dataStore.edit { it[viewModeKey] = mode.name }
        }
    }
}

enum class DocumentsViewMode { LIST, GRID }
