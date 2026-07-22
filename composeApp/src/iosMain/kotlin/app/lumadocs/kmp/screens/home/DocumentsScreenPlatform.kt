package app.lumadocs.kmp.screens.home

import androidx.compose.runtime.Composable
import app.lumadocs.kmp.services.DriveFile
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
internal actual fun openDriveFile(file: DriveFile) {
    val uriString = file.webContentLink ?: file.webViewLink ?: file.thumbnailLink
    val nsUrl = NSURL.URLWithString(uriString ?: return) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
