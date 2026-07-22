package app.lumadocs.kmp.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A live in-app camera preview. Renders the camera feed inside [modifier]; increment
 * [captureTrigger] to take a photo, which is delivered (compressed) via [onCaptured] (null on
 * failure/cancel). [flashOn] switches the torch on/off while previewing. [onPermissionDenied]
 * fires if the camera permission is refused.
 */
@Composable
expect fun InAppCameraPreview(
    modifier: Modifier,
    captureTrigger: Int,
    flashOn: Boolean,
    onCaptured: (PickedFile?) -> Unit,
    onPermissionDenied: () -> Unit,
)

/** Whether this platform provides an embedded [InAppCameraPreview]. */
expect fun inAppCameraSupported(): Boolean
