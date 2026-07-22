package app.lumadocs.kmp.screens.home

import androidx.compose.runtime.Composable
import app.lumadocs.kmp.utils.ImagePickerState

@Composable
internal actual fun setupImagePicker(): (() -> Unit)? {

    return null
}

internal actual fun getSelectedFileName(): String? {
    return ImagePickerState.selectedFileName.value
}

internal actual fun getSelectedImageBytes(): ByteArray? {
    return ImagePickerState.selectedBytes.value
}

internal actual fun getCurrentImageMimeType(): String {
    return ImagePickerState.selectedMimeType.value
}
