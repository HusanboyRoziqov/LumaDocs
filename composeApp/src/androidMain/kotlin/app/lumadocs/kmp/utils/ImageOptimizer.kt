package app.lumadocs.kmp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.min

actual object ImageOptimizer {

    actual fun compressForThumbnail(
        imageBytes: ByteArray,
        targetSizeKB: Int,
        maxWidth: Int,
        maxHeight: Int,
        startQuality: Int
    ): ByteArray {
        return try {
            Log.d("ImageOptimizer", "Compressing image for thumbnail. Original size: ${imageBytes.size / 1024} KB")

            var quality = startQuality
            var compressedBytes = compressImage(imageBytes, maxWidth, maxHeight, quality)
            var compressedSizeKB = compressedBytes.size / 1024

            Log.d("ImageOptimizer", "Compression attempt 1: quality=$quality, size=${compressedSizeKB}KB")

            while (compressedSizeKB > targetSizeKB && quality > 30) {
                quality -= 5
                compressedBytes = compressImage(imageBytes, maxWidth, maxHeight, quality)
                compressedSizeKB = compressedBytes.size / 1024
                Log.d("ImageOptimizer", "Compression attempt: quality=$quality, size=${compressedSizeKB}KB")
            }

            if (compressedSizeKB > targetSizeKB) {
                var newWidth = maxWidth
                var newHeight = maxHeight
                while (compressedSizeKB > targetSizeKB && newWidth > 240) {
                    newWidth = (newWidth * 0.85).toInt()
                    newHeight = (newHeight * 0.85).toInt()
                    compressedBytes = compressImage(imageBytes, newWidth, newHeight, quality)
                    compressedSizeKB = compressedBytes.size / 1024
                    Log.d("ImageOptimizer", "Compression with resize: ${newWidth}x${newHeight}, size=${compressedSizeKB}KB")
                }
            }

            Log.d("ImageOptimizer", "Final compressed size: ${compressedSizeKB}KB")
            compressedBytes
        } catch (e: Exception) {
            Log.e("ImageOptimizer", "Error compressing image: ${e.message}", e)

            imageBytes
        }
    }

    private fun compressImage(
        imageBytes: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): ByteArray {
        return try {

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight

            var scaleFactor = 1
            if (imageWidth > maxWidth || imageHeight > maxHeight) {
                val widthScale = imageWidth.toFloat() / maxWidth.toFloat()
                val heightScale = imageHeight.toFloat() / maxHeight.toFloat()
                scaleFactor = min(widthScale, heightScale).toInt()
                if (scaleFactor < 1) scaleFactor = 1
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = scaleFactor

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                ?: return imageBytes

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val compressedBytes = outputStream.toByteArray()

            bitmap.recycle()
            outputStream.close()

            compressedBytes
        } catch (e: Exception) {
            Log.e("ImageOptimizer", "Error in compressImage: ${e.message}", e)
            imageBytes
        }
    }

    actual fun getOptimizedThumbnailUrl(
        webViewLink: String?,
        width: Int,
        height: Int
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
            Log.e("ImageOptimizer", "Error generating thumbnail URL: ${e.message}")
            webViewLink
        }
    }
}
