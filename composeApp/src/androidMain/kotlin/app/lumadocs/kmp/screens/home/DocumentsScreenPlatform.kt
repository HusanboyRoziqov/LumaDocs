package app.lumadocs.kmp.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.lumadocs.kmp.services.DriveFile

@Composable
internal actual fun openDriveFile(file: DriveFile) {
    val context: Context = LocalContext.current

    val uriString = file.webContentLink ?: file.webViewLink ?: file.thumbnailLink

    if (!uriString.isNullOrEmpty()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}
