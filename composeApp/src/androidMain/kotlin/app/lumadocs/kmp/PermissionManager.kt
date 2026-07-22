package app.lumadocs.kmp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionManager {

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Permissions to declare/request for reading gallery images, per OS version. */
    fun getRequiredPermissions(): Array<String> {
        return when {
            // Android 14+: full images OR the "Selected photos" partial-access permission.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /** Full access to all gallery images. */
    fun hasFullAccess(context: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return granted(context, perm)
    }

    /** Android 14+ partial ("Selected photos") access: only the user-picked subset is visible. */
    fun hasPartialAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !granted(context, Manifest.permission.READ_MEDIA_IMAGES)
    }

    /** True when the app can read at least some gallery images (full or partial). */
    fun hasPermissions(context: Context): Boolean =
        hasFullAccess(context) || hasPartialAccess(context)

    fun getPermissionsToRequest(context: Context): Array<String> {
        return getRequiredPermissions().filter { !granted(context, it) }.toTypedArray()
    }
}
