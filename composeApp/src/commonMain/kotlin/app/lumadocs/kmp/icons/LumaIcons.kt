package app.lumadocs.kmp.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Semantic icon set for the candlelight design. Names mirror the prototype's `icons.jsx`
 * (`<Icon name="search"/>` → `LumaIcons.Search`). Backed by Material **Outlined** vectors, which
 * share the prototype's thin-stroke, rounded aesthetic and tint cleanly via `Icon(tint = …)`.
 */
object LumaIcons {
    val Search: ImageVector get() = Icons.Outlined.Search
    val Plus: ImageVector get() = Icons.Outlined.Add
    val Scan: ImageVector get() = Icons.Outlined.DocumentScanner
    val Grid: ImageVector get() = Icons.Outlined.GridView
    val ListView: ImageVector get() = Icons.Outlined.Reorder
    val Settings: ImageVector get() = Icons.Outlined.Settings
    val Lock: ImageVector get() = Icons.Outlined.Lock
    val Shield: ImageVector get() = Icons.Outlined.Shield
    val Cloud: ImageVector get() = Icons.Outlined.Cloud
    val Upload: ImageVector get() = Icons.Outlined.CloudUpload
    val Bell: ImageVector get() = Icons.Outlined.Notifications
    val Close: ImageVector get() = Icons.Outlined.Close
    val Back: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowBack
    val Forward: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowForward
    val Check: ImageVector get() = Icons.Outlined.Check
    val Camera: ImageVector get() = Icons.Outlined.PhotoCamera
    val Image: ImageVector get() = Icons.Outlined.Image
    val Flash: ImageVector get() = Icons.Outlined.FlashOn
    val Folder: ImageVector get() = Icons.Outlined.Folder
    val Calendar: ImageVector get() = Icons.Outlined.CalendarMonth
    val Share: ImageVector get() = Icons.Outlined.Share
    val Download: ImageVector get() = Icons.Outlined.FileDownload
    val Trash: ImageVector get() = Icons.Outlined.Delete
    val Edit: ImageVector get() = Icons.Outlined.Edit
    val Filter: ImageVector get() = Icons.Outlined.FilterList
    val Globe: ImageVector get() = Icons.Outlined.Language
    val Fingerprint: ImageVector get() = Icons.Outlined.Fingerprint
    val Face: ImageVector get() = Icons.Outlined.Face
    val Sparkle: ImageVector get() = Icons.Outlined.AutoAwesome
    val Chevron: ImageVector get() = Icons.AutoMirrored.Outlined.KeyboardArrowRight
    val ChevronDown: ImageVector get() = Icons.Outlined.KeyboardArrowDown
    val Page: ImageVector get() = Icons.Outlined.Description
    val Vault: ImageVector get() = Icons.Outlined.Inventory2
    val Key: ImageVector get() = Icons.Outlined.Key
    val Eye: ImageVector get() = Icons.Outlined.Visibility
    val Print: ImageVector get() = Icons.Outlined.Print
    val Pdf: ImageVector get() = Icons.Outlined.PictureAsPdf
    val Refresh: ImageVector get() = Icons.Outlined.Refresh
}
