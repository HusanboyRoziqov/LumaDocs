package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class IncomingFile(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
)

object IncomingFileState {
    private val _file = MutableStateFlow<IncomingFile?>(null)
    val file: StateFlow<IncomingFile?> = _file

    fun set(bytes: ByteArray, fileName: String, mimeType: String) {
        _file.value = IncomingFile(bytes, fileName, mimeType)
    }

    fun clear() {
        _file.value = null
    }
}
