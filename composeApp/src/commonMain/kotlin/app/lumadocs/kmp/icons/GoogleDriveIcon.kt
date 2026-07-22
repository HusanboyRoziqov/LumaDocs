package app.lumadocs.kmp.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Official multi-color Google Drive logo, transparent background.
val GoogleDriveIcon: ImageVector
    get() {
        if (_googleDriveIcon != null) return _googleDriveIcon!!
        _googleDriveIcon = ImageVector.Builder(
            name = "GoogleDriveIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 87.3f,
            viewportHeight = 78f
        ).apply {

            // Blue base (left)
            path(fill = SolidColor(Color(0xFF0066DA))) {
                moveTo(6.6f, 66.85f)
                lineToRelative(3.85f, 6.65f)
                curveToRelative(0.8f, 1.4f, 1.95f, 2.5f, 3.3f, 3.3f)
                lineToRelative(13.75f, -23.8f)
                horizontalLineToRelative(-27.5f)
                curveToRelative(0f, 1.55f, 0.4f, 3.1f, 1.2f, 4.5f)
                close()
            }

            // Green (left face)
            path(fill = SolidColor(Color(0xFF00AC47))) {
                moveTo(43.65f, 25f)
                lineToRelative(-13.75f, -23.8f)
                curveToRelative(-1.35f, 0.8f, -2.5f, 1.9f, -3.3f, 3.3f)
                lineToRelative(-25.4f, 44f)
                arcToRelative(9.06f, 9.06f, 0f, false, false, -1.2f, 4.5f)
                horizontalLineToRelative(27.5f)
                close()
            }

            // Red (right tip)
            path(fill = SolidColor(Color(0xFFEA4335))) {
                moveTo(73.55f, 76.8f)
                curveToRelative(1.35f, -0.8f, 2.5f, -1.9f, 3.3f, -3.3f)
                lineToRelative(1.6f, -2.75f)
                lineToRelative(7.65f, -13.25f)
                curveToRelative(0.8f, -1.4f, 1.2f, -2.95f, 1.2f, -4.5f)
                horizontalLineToRelative(-27.502f)
                lineToRelative(5.852f, 11.5f)
                close()
            }

            // Dark green (top-left fold)
            path(fill = SolidColor(Color(0xFF00832D))) {
                moveTo(43.65f, 25f)
                lineToRelative(13.75f, -23.8f)
                curveToRelative(-1.35f, -0.8f, -2.9f, -1.2f, -4.5f, -1.2f)
                horizontalLineToRelative(-18.5f)
                curveToRelative(-1.6f, 0f, -3.15f, 0.45f, -4.5f, 1.2f)
                close()
            }

            // Light blue (bottom face)
            path(fill = SolidColor(Color(0xFF2684FC))) {
                moveTo(59.8f, 53f)
                horizontalLineToRelative(-32.3f)
                lineToRelative(-13.75f, 23.8f)
                curveToRelative(1.35f, 0.8f, 2.9f, 1.2f, 4.5f, 1.2f)
                horizontalLineToRelative(50.8f)
                curveToRelative(1.6f, 0f, 3.15f, -0.45f, 4.5f, -1.2f)
                close()
            }

            // Yellow (right face)
            path(fill = SolidColor(Color(0xFFFFBA00))) {
                moveTo(73.4f, 26.5f)
                lineToRelative(-12.7f, -22f)
                curveToRelative(-0.8f, -1.4f, -1.95f, -2.5f, -3.3f, -3.3f)
                lineToRelative(-13.75f, 23.8f)
                lineToRelative(16.15f, 28f)
                horizontalLineToRelative(27.45f)
                curveToRelative(0f, -1.55f, -0.4f, -3.1f, -1.2f, -4.5f)
                close()
            }

        }.build()
        return _googleDriveIcon!!
    }

private var _googleDriveIcon: ImageVector? = null
