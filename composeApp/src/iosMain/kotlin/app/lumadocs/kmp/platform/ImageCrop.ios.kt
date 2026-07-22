package app.lumadocs.kmp.platform

import app.lumadocs.kmp.utils.toByteArray
import app.lumadocs.kmp.utils.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
actual suspend fun cropImageBytes(
    bytes: ByteArray,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    mimeType: String,
): ByteArray? = withContext(Dispatchers.Default) {
    try {
        val image = UIImage.imageWithData(bytes.toNSData()) ?: return@withContext null
        // The crop rect is normalized, so it has to be resolved against the CGImage's pixel
        // dimensions — UIImage.size is in points and would crop the wrong region on @2x/@3x.
        val source = image.CGImage ?: return@withContext null
        val w = CGImageGetWidth(source).toInt()
        val h = CGImageGetHeight(source).toInt()
        if (w <= 0 || h <= 0) return@withContext null

        val x = (left * w).roundToInt().coerceIn(0, w - 1)
        val y = (top * h).roundToInt().coerceIn(0, h - 1)
        val cropWidth = ((right - left) * w).roundToInt().coerceIn(1, w - x)
        val cropHeight = ((bottom - top) * h).roundToInt().coerceIn(1, h - y)

        val cropped = CGImageCreateWithImageInRect(
            source,
            CGRectMake(x.toDouble(), y.toDouble(), cropWidth.toDouble(), cropHeight.toDouble()),
        ) ?: return@withContext null

        val result = UIImage.imageWithCGImage(cropped)
        val data = if (mimeType.contains("png", ignoreCase = true)) {
            UIImagePNGRepresentation(result)
        } else {
            UIImageJPEGRepresentation(result, 0.9)
        }
        data?.toByteArray()
    } catch (e: Exception) {
        println("ImageCrop: crop failed: ${e.message}")
        null
    }
}
