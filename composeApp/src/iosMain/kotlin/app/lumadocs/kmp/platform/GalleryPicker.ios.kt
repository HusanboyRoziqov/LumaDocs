@file:OptIn(ExperimentalForeignApi::class)

package app.lumadocs.kmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.lumadocs.kmp.utils.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAssetResource
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.presentLimitedLibraryPickerFromViewController
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import app.lumadocs.kmp.utils.toByteArray

// Matches the Android actual's budget so uploads are comparable across platforms.
private const val TARGET_SIZE_BYTES = 500 * 1024
private const val MIN_QUALITY = 30
private const val QUALITY_STEP = 10
private const val MAX_DIMENSION = 1920.0
private const val FALLBACK_DIMENSION = 1280.0

/** Same ladder as Android: drop quality first, then fall back to a smaller image. */
private fun compressToTarget(image: UIImage): ByteArray? {
    var quality = 85
    while (quality >= MIN_QUALITY) {
        val bytes = encodeJpeg(image, MAX_DIMENSION, quality) ?: return null
        if (bytes.size <= TARGET_SIZE_BYTES) return bytes
        quality -= QUALITY_STEP
    }
    return encodeJpeg(image, FALLBACK_DIMENSION, MIN_QUALITY)
}

actual fun hasGalleryPermission(): Boolean = when (PHPhotoLibrary.authorizationStatus()) {
    PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> true
    else -> false
}

actual fun isPartialGalleryAccess(): Boolean =
    PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusLimited

@Composable
actual fun rememberGalleryPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit = {
    PHPhotoLibrary.requestAuthorization { status ->
        val granted = status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
        dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
    }
}

@Composable
actual fun rememberReselectPhotos(onResult: () -> Unit): () -> Unit = {
    dispatch_async(dispatch_get_main_queue()) {
        val presenter = topViewController()
        val library = PHPhotoLibrary.sharedPhotoLibrary()
        if (presenter == null) {
            onResult()
        } else {
            // The completion-handler variant is iOS 15+; without it we cannot know when the
            // user finished, so reload immediately and accept a possibly stale grid.
            val withCompletion = NSSelectorFromString(
                "presentLimitedLibraryPickerFromViewController:completionHandler:",
            )
            if (library.respondsToSelector(withCompletion)) {
                library.presentLimitedLibraryPickerFromViewController(presenter) { _ ->
                    dispatch_async(dispatch_get_main_queue()) { onResult() }
                }
            } else {
                library.presentLimitedLibraryPickerFromViewController(presenter)
                onResult()
            }
        }
    }
}

actual suspend fun loadGalleryImages(limit: Int): List<GalleryImage> = withContext(Dispatchers.Default) {
    newestImageAssets(limit).map { asset ->
        // The uri is resolved later by PhAssetFetcher, so no image data is read here.
        GalleryImage(id = asset.localIdentifier, uri = phAssetUri(asset.localIdentifier))
    }
}

actual suspend fun readGalleryImage(uri: String): PickedFile? = withContext(Dispatchers.Default) {
    val identifier = localIdentifierFrom(uri) ?: return@withContext null
    val asset = assetFor(identifier) ?: return@withContext null
    val image = loadAssetImage(asset) ?: return@withContext null
    val bytes = compressToTarget(image) ?: return@withContext null

    val originalName = PHAssetResource.assetResourcesForAsset(asset)
        .filterIsInstance<PHAssetResource>()
        .firstOrNull()
        ?.originalFilename

    PickedFile(
        // Always re-encoded as JPEG above, so the extension must follow.
        fileName = originalName?.substringBeforeLast('.')?.plus(".jpg") ?: "photo_${asset.localIdentifier.take(8)}.jpg",
        mimeType = "image/jpeg",
        bytes = bytes,
    )
}

/**
 * UIKit keeps delegates weakly, so a picker's delegate must be retained here or it would be
 * collected the moment the composable's call returns and the callbacks would never fire.
 */
private var activePickerDelegate: NSObject? = null

@OptIn(ExperimentalForeignApi::class)
private class CameraDelegate(
    private val onCaptured: (PickedFile?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) {
            activePickerDelegate = null
            val bytes = image?.let { compressToTarget(it) }
            onCaptured(
                bytes?.let {
                    PickedFile(fileName = "camera_${nowMillis()}.jpg", mimeType = "image/jpeg", bytes = it)
                },
            )
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            activePickerDelegate = null
            onCaptured(null)
        }
    }
}

private class FilePickerDelegate(
    private val onPicked: (List<PickedFile>) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        activePickerDelegate = null
        val files = didPickDocumentsAtURLs.filterIsInstance<NSURL>().mapNotNull { url ->
            // Files outside the app sandbox need an explicit security scope to be readable.
            val scoped = url.startAccessingSecurityScopedResource()
            try {
                val data = NSData.dataWithContentsOfURL(url) ?: return@mapNotNull null
                PickedFile(
                    fileName = url.lastPathComponent ?: "document",
                    mimeType = mimeTypeFor(url.pathExtension),
                    bytes = data.toByteArray(),
                )
            } finally {
                if (scoped) url.stopAccessingSecurityScopedResource()
            }
        }
        if (files.isNotEmpty()) onPicked(files)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        activePickerDelegate = null
    }
}

private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

private fun mimeTypeFor(extension: String?): String = when (extension?.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "heic" -> "image/heic"
    "pdf" -> "application/pdf"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "txt" -> "text/plain"
    else -> "application/octet-stream"
}

@Composable
actual fun rememberFilePicker(
    mimeTypes: Array<String>,
    onPicked: (List<PickedFile>) -> Unit,
): () -> Unit {
    val docTypes = mimeTypes.flatMap { m ->
        when {
            m == "application/pdf" -> listOf("com.adobe.pdf")
            m.startsWith("image/") -> listOf("public.image")
            else -> listOf("public.item")
        }
    }.distinct().ifEmpty { listOf("public.item") }
    val launch = remember(onPicked, mimeTypes.contentToString()) {
        {
            dispatch_async(dispatch_get_main_queue()) {
                val presenter = topViewController()
                if (presenter != null) {
                    val delegate = FilePickerDelegate(onPicked)
                    activePickerDelegate = delegate
                    val controller = UIDocumentPickerViewController(
                        documentTypes = docTypes,
                        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
                    ).apply {
                        this.delegate = delegate
                        allowsMultipleSelection = true
                    }
                    presenter.presentViewController(controller, animated = true, completion = null)
                }
            }
        }
    }
    return launch
}

@Composable
actual fun rememberCameraCapture(onCaptured: (PickedFile?) -> Unit): () -> Unit {
    val launch = remember(onCaptured) {
        {
            dispatch_async(dispatch_get_main_queue()) {
                val presenter = topViewController()
                val cameraAvailable = UIImagePickerController.isSourceTypeAvailable(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                )
                if (presenter == null || !cameraAvailable) {
                    // No camera (e.g. simulator): report cancellation rather than hanging.
                    onCaptured(null)
                } else {
                    val delegate = CameraDelegate(onCaptured)
                    activePickerDelegate = delegate
                    val controller = UIImagePickerController().apply {
                        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                        this.delegate = delegate
                    }
                    presenter.presentViewController(controller, animated = true, completion = null)
                }
            }
        }
    }
    return launch
}
