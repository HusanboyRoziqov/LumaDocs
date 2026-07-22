package app.lumadocs.kmp.platform

import androidx.compose.runtime.Composable

/** A single image from the device gallery, identified by its content URI. */
data class GalleryImage(val id: String, val uri: String)

/** A file (image or document) ready to be added to the upload list. */
data class PickedFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

/** Whether the app currently has permission to read the device gallery. */
expect fun hasGalleryPermission(): Boolean

/**
 * True when the app only has partial ("Selected photos") gallery access (Android 14+),
 * i.e. it can see just the subset the user picked and should offer a way to select more.
 */
expect fun isPartialGalleryAccess(): Boolean

/**
 * Re-opens the system photo selection so the user can change/add which photos are shared
 * with the app (relevant when access is partial). Invokes [onResult] when done.
 */
@Composable
expect fun rememberReselectPhotos(onResult: () -> Unit): () -> Unit

/** Loads the most recent gallery image content-URIs (newest first). */
expect suspend fun loadGalleryImages(limit: Int = 300): List<GalleryImage>

/** Reads and compresses a gallery image (by its content URI) into upload bytes. */
expect suspend fun readGalleryImage(uri: String): PickedFile?

/** Requests gallery read permission; invokes [onResult] with the outcome. */
@Composable
expect fun rememberGalleryPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit

/**
 * Opens the system file picker (multiple) filtered to [mimeTypes] and returns the picked files.
 * Defaults to any type. Pass image mime types for the gallery, or the PDF mime type for documents.
 */
@Composable
expect fun rememberFilePicker(
    mimeTypes: Array<String> = arrayOf("*/*"),
    onPicked: (List<PickedFile>) -> Unit,
): () -> Unit

/** Opens the camera to take a photo; returns the captured (compressed) image, or null if cancelled. */
@Composable
expect fun rememberCameraCapture(onCaptured: (PickedFile?) -> Unit): () -> Unit
