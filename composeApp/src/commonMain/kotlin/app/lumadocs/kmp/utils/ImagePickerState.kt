package app.lumadocs.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ImagePickerState {
    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _selectedMimeType = MutableStateFlow<String>("image/jpeg")
    val selectedMimeType: StateFlow<String> = _selectedMimeType.asStateFlow()

    private val _selectedBytes = MutableStateFlow<ByteArray?>(null)
    val selectedBytes: StateFlow<ByteArray?> = _selectedBytes.asStateFlow()

    private val _selectedUri = MutableStateFlow<String?>(null)
    val selectedUri: StateFlow<String?> = _selectedUri.asStateFlow()

    fun updateImageSelection(
        fileName: String?,
        mimeType: String = "image/jpeg",
        bytes: ByteArray? = null,
        uri: String? = null
    ) {
        _selectedFileName.value = fileName
        _selectedMimeType.value = mimeType
        _selectedBytes.value = bytes
        _selectedUri.value = uri
    }

    fun clearSelection() {
        _selectedFileName.value = null
        _selectedMimeType.value = "image/jpeg"
        _selectedBytes.value = null
        _selectedUri.value = null
    }
}

@Composable
fun observeImagePickerState(): ImagePickerStateObserver {
    val fileName by ImagePickerState.selectedFileName.collectAsState()
    val mimeType by ImagePickerState.selectedMimeType.collectAsState()
    val bytes by ImagePickerState.selectedBytes.collectAsState()
    val uri by ImagePickerState.selectedUri.collectAsState()

    return ImagePickerStateObserver(
        fileName = fileName,
        mimeType = mimeType,
        bytes = bytes,
        uri = uri
    )
}

class ImagePickerStateObserver(
    val fileName: String? = null,
    val mimeType: String = "image/jpeg",
    val bytes: ByteArray? = null,
    val uri: String? = null
)
