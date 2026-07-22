package app.lumadocs.kmp.screens.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.lumadocs.kmp.ImageUploadUtils
import app.lumadocs.kmp.PermissionManager
import app.lumadocs.kmp.utils.ImagePickerState

private const val TARGET_SIZE_BYTES = 500 * 1024
private const val MIN_QUALITY = 30
private const val QUALITY_STEP = 10

private fun compressToTargetSize(
    context: Context,
    uri: Uri,
    maxWidth: Int = 1920,
    maxHeight: Int = 1920,
    startQuality: Int = 85,
): ByteArray? {
    var quality = startQuality

    while (quality >= MIN_QUALITY) {
        val bytes = ImageUploadUtils.readAndCompressImageFromUri(
            context = context,
            uri = uri,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            quality = quality
        )

        if (bytes == null) return null

        if (bytes.size <= TARGET_SIZE_BYTES) {
            return bytes
        }

        quality -= QUALITY_STEP
    }

    return ImageUploadUtils.readAndCompressImageFromUri(
        context = context,
        uri = uri,
        maxWidth = 1280,
        maxHeight = 1280,
        quality = MIN_QUALITY
    )
}

@Composable
internal actual fun setupImagePicker(): (() -> Unit)? {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = ImageUploadUtils.getFileNameFromUri(context, uri)
            val mimeType = ImageUploadUtils.getMimeType(context, uri)

            val fileBytes = compressToTargetSize(
                context = context,
                uri = uri,
                maxWidth = 1920,
                maxHeight = 1920,
                startQuality = 85
            )

            ImagePickerState.updateImageSelection(
                fileName = fileName,
                mimeType = mimeType,
                bytes = fileBytes,
                uri = uri.toString()
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        imagePickerLauncher.launch("image/*")
    }

    return {
        if (PermissionManager.hasPermissions(context)) {
            imagePickerLauncher.launch("image/*")
        } else {
            val permissionsToRequest = PermissionManager.getPermissionsToRequest(context)
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest)
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }
    }
}

internal actual fun getSelectedImageBytes(): ByteArray? = ImagePickerState.selectedBytes.value
internal actual fun getSelectedFileName(): String? = ImagePickerState.selectedFileName.value
internal actual fun getCurrentImageMimeType(): String = ImagePickerState.selectedMimeType.value
