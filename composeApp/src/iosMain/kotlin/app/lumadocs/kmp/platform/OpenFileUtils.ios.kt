package app.lumadocs.kmp.platform

import app.lumadocs.kmp.utils.toNSData
import app.lumadocs.kmp.utils.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * UIKit holds the interaction controller and its delegate weakly, so without these strong
 * references both would deallocate as soon as this function returns and the preview would
 * never appear. Cleared when the preview is dismissed.
 */
private var activeController: UIDocumentInteractionController? = null
private var activeDelegate: PreviewDelegate? = null

private class PreviewDelegate(
    private val presenter: UIViewController,
) : NSObject(), UIDocumentInteractionControllerDelegateProtocol {

    override fun documentInteractionControllerViewControllerForPreview(
        controller: UIDocumentInteractionController,
    ): UIViewController = presenter

    override fun documentInteractionControllerDidEndPreview(
        controller: UIDocumentInteractionController,
    ) {
        activeController = null
        activeDelegate = null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun openFileExternally(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
    val safeName = fileName.substringAfterLast('/').ifBlank { "document" }
    val path = NSTemporaryDirectory() + safeName
    if (!bytes.toNSData().writeToFile(path, atomically = true)) return false

    dispatch_async(dispatch_get_main_queue()) {
        val presenter = topViewController() ?: return@dispatch_async
        val controller = UIDocumentInteractionController.interactionControllerWithURL(
            NSURL.fileURLWithPath(path),
        )
        val delegate = PreviewDelegate(presenter)
        controller.delegate = delegate
        activeController = controller
        activeDelegate = delegate

        // Fall back to the "Open in…" menu for types QuickLook cannot render itself.
        if (!controller.presentPreviewAnimated(true)) {
            controller.presentOpenInMenuFromRect(
                rect = presenter.view.bounds,
                inView = presenter.view,
                animated = true,
            )
        }
    }
    return true
}
