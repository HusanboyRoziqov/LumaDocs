package app.lumadocs.kmp.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GoogleIcon: ImageVector
    get() {
        if (_googleIcon != null) return _googleIcon!!
        _googleIcon = ImageVector.Builder(
            name = "GoogleIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            path(
                fill = SolidColor(Color(0xFF4285F4)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(22.56f, 12.25f)
                curveTo(22.56f, 11.46f, 22.49f, 10.71f, 22.37f, 9.98f)
                horizontalLineTo(12f)
                verticalLineTo(14.28f)
                horizontalLineTo(17.92f)
                curveTo(17.66f, 15.65f, 16.89f, 16.81f, 15.72f, 17.59f)
                verticalLineTo(20.34f)
                horizontalLineTo(19.29f)
                curveTo(21.37f, 18.42f, 22.56f, 15.6f, 22.56f, 12.25f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFF34A853)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 23f)
                curveTo(14.97f, 23f, 17.46f, 22.02f, 19.28f, 20.34f)
                lineTo(15.71f, 17.59f)
                curveTo(14.72f, 18.25f, 13.45f, 18.64f, 12f, 18.64f)
                curveTo(9.15f, 18.64f, 6.73f, 16.72f, 5.87f, 14.13f)
                horizontalLineTo(2.18f)
                verticalLineTo(16.97f)
                curveTo(3.99f, 20.53f, 7.7f, 23f, 12f, 23f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFFFBBC05)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(5.87f, 14.13f)
                curveTo(5.65f, 13.47f, 5.52f, 12.77f, 5.52f, 12.04f)
                curveTo(5.52f, 11.31f, 5.65f, 10.61f, 5.87f, 9.95f)
                verticalLineTo(7.11f)
                horizontalLineTo(2.18f)
                curveTo(1.43f, 8.55f, 1f, 10.22f, 1f, 12f)
                curveTo(1f, 13.78f, 1.43f, 15.45f, 2.18f, 16.89f)
                lineTo(5.87f, 14.13f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFFEA4335)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 5.38f)
                curveTo(13.62f, 5.38f, 15.06f, 5.94f, 16.21f, 7.02f)
                lineTo(19.36f, 3.87f)
                curveTo(17.45f, 2.09f, 14.97f, 1f, 12f, 1f)
                curveTo(7.7f, 1f, 3.99f, 3.47f, 2.18f, 7.11f)
                lineTo(5.87f, 9.87f)
                curveTo(6.73f, 7.28f, 9.15f, 5.38f, 12f, 5.38f)
                close()
            }

        }.build()
        return _googleIcon!!
    }

private var _googleIcon: ImageVector? = null
