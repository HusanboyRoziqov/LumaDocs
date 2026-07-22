package app.lumadocs.kmp.platform

import android.content.Intent
import androidx.core.content.FileProvider
import app.lumadocs.kmp.LumaDocsApplication
import java.io.File
import java.io.FileOutputStream

actual fun openFileExternally(bytes: ByteArray, fileName: String, mimeType: String): Boolean {
    val context = LumaDocsApplication.instance
    return try {
        val safeName = fileName.substringAfterLast('/').ifBlank { "document" }
        val file = File(context.cacheDir, safeName)
        FileOutputStream(file).use { it.write(bytes) }

        val uri = FileProvider.getUriForFile(context, "app.lumadocs.kmp.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
