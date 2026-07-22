package app.lumadocs.kmp.platform

import app.lumadocs.kmp.utils.toNSData
import app.lumadocs.kmp.utils.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual fun shareContent(bytes: ByteArray?, filename: String?, text: String?): Boolean {
    val item: Any = when {
        bytes != null -> {
            // UIActivityViewController shares files by URL, so the bytes have to land on
            // disk first. Temp dir is fine: the share sheet reads it before iOS reclaims it.
            val path = NSTemporaryDirectory() + (filename ?: "shared_image.jpg")
            if (!bytes.toNSData().writeToFile(path, atomically = true)) return false
            NSURL.fileURLWithPath(path)
        }

        !text.isNullOrEmpty() -> text
        else -> return false
    }

    dispatch_async(dispatch_get_main_queue()) {
        val presenter = topViewController() ?: return@dispatch_async
        val controller = UIActivityViewController(
            activityItems = listOf(item),
            applicationActivities = null,
        )
        // On iPad the sheet is a popover and must have an anchor, or presenting it throws.
        controller.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = CGRectMake(
                presenter.view.bounds.useContents { size.width } / 2.0,
                presenter.view.bounds.useContents { size.height } / 2.0,
                0.0,
                0.0,
            )
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }
    return true
}
