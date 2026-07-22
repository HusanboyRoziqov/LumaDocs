package app.lumadocs.kmp.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
actual object ImageOptimizer {

    /**
     * Mirrors the Android actual's strategy: drop JPEG quality first, and only start
     * shrinking the image once quality alone can't reach [targetSizeKB].
     */
    actual fun compressForThumbnail(
        imageBytes: ByteArray,
        targetSizeKB: Int,
        maxWidth: Int,
        maxHeight: Int,
        startQuality: Int,
    ): ByteArray {
        return try {
            val image = UIImage.imageWithData(imageBytes.toNSData()) ?: return imageBytes

            var quality = startQuality
            var compressed = compress(image, maxWidth, maxHeight, quality) ?: return imageBytes

            while (compressed.size / 1024 > targetSizeKB && quality > 30) {
                quality -= 5
                compressed = compress(image, maxWidth, maxHeight, quality) ?: return compressed
            }

            if (compressed.size / 1024 > targetSizeKB) {
                var width = maxWidth
                var height = maxHeight
                while (compressed.size / 1024 > targetSizeKB && width > 240) {
                    width = (width * 0.85).toInt()
                    height = (height * 0.85).toInt()
                    compressed = compress(image, width, height, quality) ?: return compressed
                }
            }

            compressed
        } catch (e: Exception) {
            println("ImageOptimizer: compression failed: ${e.message}")
            imageBytes
        }
    }

    /** Scales [image] to fit the bounds (never upscaling) and encodes it as JPEG. */
    private fun compress(image: UIImage, maxWidth: Int, maxHeight: Int, quality: Int): ByteArray? {
        val (sourceWidth, sourceHeight) = image.size.useContents { width to height }
        if (sourceWidth <= 0.0 || sourceHeight <= 0.0) return null

        val scale = min(maxWidth / sourceWidth, maxHeight / sourceHeight).coerceAtMost(1.0)
        val targetWidth = sourceWidth * scale
        val targetHeight = sourceHeight * scale

        // scale = 1.0 keeps the output in pixels, so maxWidth/maxHeight mean what they say
        // regardless of the device's screen density.
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val scaled = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return UIImageJPEGRepresentation(scaled ?: image, quality / 100.0)?.toByteArray()
    }

    actual fun getOptimizedThumbnailUrl(
        webViewLink: String?,
        width: Int,
        height: Int,
    ): String? {
        if (webViewLink.isNullOrEmpty()) return null

        return try {
            when {
                webViewLink.contains("drive.google.com") -> {
                    val fileId = webViewLink.substringAfterLast("/d/").substringBefore("/")
                    if (fileId.isNotEmpty()) {
                        "https://drive.google.com/thumbnail?id=$fileId&sz=${maxOf(width, height)}"
                    } else {
                        webViewLink
                    }
                }

                else -> webViewLink
            }
        } catch (e: Exception) {
            println("ImageOptimizer: thumbnail url failed: ${e.message}")
            webViewLink
        }
    }
}
