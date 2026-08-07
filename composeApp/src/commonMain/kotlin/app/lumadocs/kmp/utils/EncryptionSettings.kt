package app.lumadocs.kmp.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Whether new documents are encrypted (AES-256 via [SecurityUtils]) before they leave the device.
 *
 * This is the app-wide default the Settings screen owns; the add/scan flows read it when a file is
 * picked, and the per-upload switch can still override it for a single batch. It only applies to
 * new uploads — documents already in Drive keep whatever state they were stored with.
 */
object EncryptionSettings {

    /** On by default: Settings has always advertised encryption as active. */
    const val DEFAULT = true

    private val key = booleanPreferencesKey("encrypt_new_uploads_v1")

    fun flow(dataStore: DataStore<Preferences>): Flow<Boolean> =
        dataStore.data.map { it[key] ?: DEFAULT }

    suspend fun get(dataStore: DataStore<Preferences>): Boolean =
        runCatching { dataStore.data.first()[key] ?: DEFAULT }.getOrDefault(DEFAULT)

    suspend fun set(dataStore: DataStore<Preferences>, enabled: Boolean) {
        runCatching { dataStore.edit { it[key] = enabled } }
    }
}
