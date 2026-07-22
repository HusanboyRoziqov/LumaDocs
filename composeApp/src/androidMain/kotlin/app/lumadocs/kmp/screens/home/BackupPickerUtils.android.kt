package app.lumadocs.kmp.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberBackupFilePicker(onResult: (uriString: String?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onResult(uri?.toString())
    }
    return { launcher.launch("*/*") }
}
