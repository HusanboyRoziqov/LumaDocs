package app.lumadocs.kmp.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bmp?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
