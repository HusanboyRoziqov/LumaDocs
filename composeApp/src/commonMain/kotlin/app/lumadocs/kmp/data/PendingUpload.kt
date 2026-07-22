package app.lumadocs.kmp.data

import kotlinx.serialization.Serializable

/**
 * A document the user "uploaded" while offline: its bytes are stored locally (cache) and it's shown
 * in the vault with a pending badge until it can be pushed to Google Drive.
 */
@Serializable
data class PendingUpload(
    val id: String,
    val name: String,
    val mimeType: String,
    val category: String? = null,
    val expiryDate: String? = null,
    val encrypted: Boolean = false,
    val sizeBytes: Long = 0L,
)
