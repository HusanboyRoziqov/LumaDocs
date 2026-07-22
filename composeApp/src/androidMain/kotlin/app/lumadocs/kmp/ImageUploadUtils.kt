package app.lumadocs.kmp

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream

object ImageUploadUtils {

    fun getMimeType(context: Context, uri: Uri): String {
        return when {
            uri.scheme == ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.getType(uri) ?: "application/octet-stream"
            }
            else -> {
                val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            }
        }
    }

    fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error reading file: ${e.message}", e)
            null
        }
    }

    fun readAndCompressImageFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1920,
        maxHeight: Int = 1920,
        quality: Int = 85,
    ): ByteArray? {
        return try {
            Log.d("ImageCompress", "Compressing: quality=$quality, maxSize=${maxWidth}x${maxHeight}")

            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            Log.d("ImageCompress", "Original dimensions: ${original.width}x${original.height}")

            val scaled = scaleBitmap(original, maxWidth, maxHeight)
            if (scaled !== original) original.recycle()

            Log.d("ImageCompress", "Scaled dimensions: ${scaled.width}x${scaled.height}")

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            scaled.recycle()

            val result = outputStream.toByteArray()
            outputStream.close()

            Log.d("ImageCompress", "Compressed size: ${result.size / 1024} KB at quality=$quality")

            result
        } catch (e: Exception) {
            Log.e("ImageCompress", "Error compressing image: ${e.message}", e)
            readBytesFromUri(context, uri)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) return bitmap

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        Log.d("ImageCompress", "Scaling $width x $height -> $newWidth x $newHeight (ratio=$ratio)")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (nameIndex >= 0) {
                it.moveToFirst()
                name = it.getString(nameIndex)
            }
        }
        return name.ifEmpty { "image_${System.currentTimeMillis()}.jpg" }
    }
}
