package app.lumadocs.kmp.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

actual suspend fun cropImageBytes(
    bytes: ByteArray,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    mimeType: String,
): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        val w = src.width
        val h = src.height

        val x = (left * w).roundToInt().coerceIn(0, w - 1)
        val y = (top * h).roundToInt().coerceIn(0, h - 1)
        val cw = ((right - left) * w).roundToInt().coerceIn(1, w - x)
        val ch = ((bottom - top) * h).roundToInt().coerceIn(1, h - y)

        val cropped = Bitmap.createBitmap(src, x, y, cw, ch)
        val format =
            if (mimeType.contains("png", ignoreCase = true)) Bitmap.CompressFormat.PNG
            else Bitmap.CompressFormat.JPEG

        val out = ByteArrayOutputStream()
        cropped.compress(format, 90, out)
        if (cropped !== src) src.recycle()
        cropped.recycle()
        out.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
