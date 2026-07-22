package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object IncomingBackupState {
    private val _uri = MutableStateFlow<String?>(null)
    val uri: StateFlow<String?> = _uri

    fun set(uriString: String) { _uri.value = uriString }
    fun clear() { _uri.value = null }
}
