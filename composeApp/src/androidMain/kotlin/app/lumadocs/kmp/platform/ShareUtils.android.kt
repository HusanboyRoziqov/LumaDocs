package app.lumadocs.kmp.platform

import android.content.Intent
import androidx.core.content.FileProvider
import app.lumadocs.kmp.LumaDocsApplication
import java.io.File
import java.io.FileOutputStream

actual fun shareContent(bytes: ByteArray?, filename: String?, text: String?): Boolean {
    val context = LumaDocsApplication.instance
    try {
        if (bytes != null) {
            val name = filename ?: "shared_image.jpg"
            val file = File(context.cacheDir, name)
            FileOutputStream(file).use { it.write(bytes) }
            val uri = FileProvider.getUriForFile(context, "app.lumadocs.kmp.fileprovider", file)
            val mimeType = when {
                name.endsWith(".lumadocs") -> "application/x-lumadocs"
                else -> {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/*"
                }
            }
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        } else if (!text.isNullOrEmpty()) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(intent, "Share")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}
