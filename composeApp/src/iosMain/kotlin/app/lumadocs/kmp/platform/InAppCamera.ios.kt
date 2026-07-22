package app.lumadocs.kmp.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS does not have an embedded live preview yet — [inAppCameraSupported] returns false so the
 * scan screen falls back to the system camera. (AVFoundation preview is a follow-up.)
 */
actual fun inAppCameraSupported(): Boolean = false

@Composable
actual fun InAppCameraPreview(
    modifier: Modifier,
    captureTrigger: Int,
    flashOn: Boolean,
    onCaptured: (PickedFile?) -> Unit,
    onPermissionDenied: () -> Unit,
) {
    Box(modifier)
}
