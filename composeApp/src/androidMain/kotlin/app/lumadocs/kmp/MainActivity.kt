package app.lumadocs.kmp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import app.lumadocs.kmp.utils.SecurityUtils
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent system bars so the app's own background (nBlack100) shows
        // through. The bar icon contrast follows the app theme, not the device —
        // see SystemBarsAppearance() in LumaDocs().
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides this
            ) {
                LumaDocs()
            }
        }

        intent?.getStringExtra("openFileId")?.let { OpenFileState.set(it) }
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("openFileId")?.let { OpenFileState.set(it) }
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch {
            try {
                val mimeType = intent.type
                    ?: contentResolver.getType(uri)
                    ?: "application/octet-stream"

                val displayName = withContext(Dispatchers.IO) {
                    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                }
                val rawName = displayName ?: uri.lastPathSegment ?: "document"

                val isZipMagic = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { s ->
                        val buf = ByteArray(4)
                        val read = s.read(buf)
                        read == 4 && buf[0] == 0x50.toByte() && buf[1] == 0x4B.toByte() &&
                            buf[2] == 0x03.toByte() && buf[3] == 0x04.toByte()
                    } ?: false
                }

                when {

                    rawName.endsWith(".lumadocsbackup") ||
                        mimeType == "application/x-lumadocsbackup" ||
                        (isZipMagic && !mimeType.startsWith("image/") && mimeType != "application/pdf") -> {

                        val cacheFile = File(cacheDir, "incoming_backup.lumadocsbackup")
                        val copied = withContext(Dispatchers.IO) {
                            runCatching {
                                contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                                }
                            }.isSuccess
                        }
                        if (copied) IncomingBackupState.set(cacheFile.absolutePath)
                    }

                    rawName.endsWith(".lumadocs") || mimeType == "application/x-lumadocs" -> {
                        val raw = withContext(Dispatchers.IO) {
                            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } ?: return@launch
                        val bytes = withContext(Dispatchers.Default) { SecurityUtils.decrypt(raw) }
                        val name = rawName.removeSuffix(".lumadocs")
                        IncomingFileState.set(bytes, name, "image/jpeg")
                    }

                    else -> {
                        val raw = withContext(Dispatchers.IO) {
                            contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } ?: return@launch
                        IncomingFileState.set(raw, rawName, mimeType)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Preview
@Composable
fun LumaDocsAndroidPreview() {
    LumaDocs()
}
