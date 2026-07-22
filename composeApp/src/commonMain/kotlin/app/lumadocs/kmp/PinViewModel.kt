package app.lumadocs.kmp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.utils.SecurityUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * App-lock PIN, stored on-device only. The PIN is never kept in plain text: it is
 * AES-encrypted (via [SecurityUtils]) and verified by decrypting and comparing.
 */
@OptIn(ExperimentalEncodingApi::class)
class PinViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val pinKey = stringPreferencesKey("pin_cipher_v1")
    private val lockKey = booleanPreferencesKey("pin_lock_enabled")

    /** True when app-lock is on AND a PIN is actually set. */
    val lockEnabled = dataStore.data
        .map { prefs -> (prefs[lockKey] ?: false) && prefs[pinKey] != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, readLockEnabled())

    private fun readLockEnabled(): Boolean = runCatching {
        runBlocking {
            val prefs = dataStore.data.first()
            (prefs[lockKey] ?: false) && prefs[pinKey] != null
        }
    }.getOrDefault(false)

    suspend fun hasPin(): Boolean =
        runCatching { dataStore.data.first()[pinKey] != null }.getOrDefault(false)

    suspend fun setPin(pin: String) {
        val cipher = Base64.encode(SecurityUtils.encrypt(pin.encodeToByteArray()))
        dataStore.edit {
            it[pinKey] = cipher
            it[lockKey] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean = runCatching {
        val cipher = dataStore.data.first()[pinKey] ?: return false
        val stored = SecurityUtils.decrypt(Base64.decode(cipher)).decodeToString()
        stored == pin
    }.getOrDefault(false)

    /** Turns off app-lock and forgets the stored PIN. */
    fun disableLock() {
        viewModelScope.launch {
            dataStore.edit {
                it.remove(pinKey)
                it[lockKey] = false
            }
        }
    }
}
