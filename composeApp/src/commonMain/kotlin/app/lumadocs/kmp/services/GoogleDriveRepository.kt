package app.lumadocs.kmp.services

import kotlinx.serialization.Serializable

@Serializable
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val description: String? = null,
    val createdTime: String? = null,
    val modifiedTime: String? = null,
    val size: Long? = null,
    val parentId: String? = null,
    val thumbnailLink: String? = null,
    val webViewLink: String? = null,
    val webContentLink: String? = null,
    val category: String? = null,
    val encrypted: Boolean = false,
    val expiryDate: String? = null,
)

interface GoogleDriveRepository {
    suspend fun uploadFile(
        fileName: String,
        mimeType: String,
        fileContent: ByteArray,
        parentFolderId: String? = null,
        description: String? = null,
        category: String? = null,
        makeEncrypted: Boolean = false,
        expiryDate: String? = null,
    ): UploadResult

    suspend fun createFolder(folderName: String, parentFolderId: String? = null): String?

    suspend fun listFiles(folderId: String? = null, query: String = ""): List<DriveFile>

    suspend fun deleteFile(fileId: String): Boolean

    suspend fun getFileContent(fileId: String, isEncrypted: Boolean = false): ByteArray?

    /**
     * Persists metadata changes for an existing file to Drive.
     * A null [expiryDate] removes the stored expiry app property.
     */
    suspend fun updateFileMetadata(
        fileId: String,
        name: String,
        description: String?,
        expiryDate: String?,
    ): Boolean
}

data class UploadResult(
    val success: Boolean,
    val fileId: String? = null,
    val errorMessage: String? = null,
)
