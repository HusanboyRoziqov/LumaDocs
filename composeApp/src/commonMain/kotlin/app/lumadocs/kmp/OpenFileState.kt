package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object OpenFileState {
    private val _fileId = MutableStateFlow<String?>(null)
    val fileId: StateFlow<String?> = _fileId

    fun set(fileId: String) { _fileId.value = fileId }
    fun clear() { _fileId.value = null }
}
