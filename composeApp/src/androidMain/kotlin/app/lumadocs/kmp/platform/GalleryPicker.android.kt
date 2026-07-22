package app.lumadocs.kmp.platform

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import app.lumadocs.kmp.ImageUploadUtils
import app.lumadocs.kmp.LumaDocsApplication
import app.lumadocs.kmp.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TARGET_SIZE_BYTES = 500 * 1024
private const val MIN_QUALITY = 30
private const val QUALITY_STEP = 10

actual fun hasGalleryPermission(): Boolean =
    PermissionManager.hasPermissions(LumaDocsApplication.instance)

actual fun isPartialGalleryAccess(): Boolean =
    PermissionManager.hasPartialAccess(LumaDocsApplication.instance)

@Composable
actual fun rememberReselectPhotos(onResult: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> onResult() }
    // Re-requesting the media permissions while in partial mode re-opens the
    // system "Select photos" UI so the user can update their selection.
    return { launcher.launch(PermissionManager.getRequiredPermissions()) }
}

actual suspend fun loadGalleryImages(limit: Int): List<GalleryImage> = withContext(Dispatchers.IO) {
    val context = LumaDocsApplication.instance
    val images = ArrayList<GalleryImage>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && images.size < limit) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                images.add(GalleryImage(id = id.toString(), uri = uri.toString()))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    images
}

actual suspend fun readGalleryImage(uri: String): PickedFile? = withContext(Dispatchers.IO) {
    val context = LumaDocsApplication.instance
    val parsed = Uri.parse(uri)
    val bytes = compressToTarget(context, parsed) ?: return@withContext null
    PickedFile(
        fileName = ImageUploadUtils.getFileNameFromUri(context, parsed),
        mimeType = ImageUploadUtils.getMimeType(context, parsed),
        bytes = bytes,
    )
}

private fun compressToTarget(context: Context, uri: Uri): ByteArray? {
    var quality = 85
    while (quality >= MIN_QUALITY) {
        val bytes = ImageUploadUtils.readAndCompressImageFromUri(context, uri, 1920, 1920, quality)
            ?: return null
        if (bytes.size <= TARGET_SIZE_BYTES) return bytes
        quality -= QUALITY_STEP
    }
    return ImageUploadUtils.readAndCompressImageFromUri(context, uri, 1280, 1280, MIN_QUALITY)
}

@Composable
actual fun rememberGalleryPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LumaDocsApplication.instance
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onResult(result.values.all { it } || PermissionManager.hasPermissions(context))
    }
    return {
        val toRequest = PermissionManager.getPermissionsToRequest(context)
        if (toRequest.isEmpty()) onResult(true) else launcher.launch(toRequest)
    }
}

@Composable
actual fun rememberFilePicker(
    mimeTypes: Array<String>,
    onPicked: (List<PickedFile>) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        val context = LumaDocsApplication.instance
        val files = uris.mapNotNull { uri ->
            val mime = ImageUploadUtils.getMimeType(context, uri)
            // Compress picked images like the camera path does (~500 KB, max 1920px):
            // raw multi-MB photos make every downstream step (decode, equals, upload) slow.
            val bytes = if (mime.startsWith("image/")) {
                compressToTarget(context, uri) ?: ImageUploadUtils.readBytesFromUri(context, uri)
            } else {
                ImageUploadUtils.readBytesFromUri(context, uri)
            } ?: return@mapNotNull null
            PickedFile(
                fileName = ImageUploadUtils.getFileNameFromUri(context, uri),
                mimeType = if (mime.startsWith("image/")) "image/jpeg" else mime,
                bytes = bytes,
            )
        }
        if (files.isNotEmpty()) onPicked(files)
    }
    return { launcher.launch(if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes) }
}

@Composable
actual fun rememberCameraCapture(onCaptured: (PickedFile?) -> Unit): () -> Unit {
    val context = LumaDocsApplication.instance
    val scope = rememberCoroutineScope()
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUri
        val file = pendingFile
        if (success && uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { compressToTarget(context, uri) }
                file?.delete()
                onCaptured(
                    bytes?.let {
                        PickedFile(
                            fileName = "camera_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg",
                            bytes = it,
                        )
                    }
                )
            }
        } else {
            file?.delete()
            onCaptured(null)
        }
    }

    return {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "app.lumadocs.kmp.fileprovider", file)
        pendingFile = file
        pendingUri = uri
        launcher.launch(uri)
    }
}
