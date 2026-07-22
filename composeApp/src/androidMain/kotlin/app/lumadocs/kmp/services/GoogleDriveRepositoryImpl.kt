package app.lumadocs.kmp.services

import android.content.Context
import android.content.Intent
import android.util.Log
import app.lumadocs.kmp.LumaDocsApplication
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveRepositoryImpl(private val context: Context) : GoogleDriveRepository {

    private var driveService: Drive? = null
    private var currentServiceEmail: String? = null

    private suspend fun ensureServiceInitialized(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Log.d("GoogleDrive", "No authenticated user found. Clearing service.")
            driveService = null
            currentServiceEmail = null
            return@withContext false
        }

        val email = currentUser.email ?: return@withContext false

        if (driveService != null && currentServiceEmail == email) {
            return@withContext true
        }

        return@withContext try {
            // Only the non-sensitive per-file scope: grants access to files this app
            // creates/opens. Avoids the restricted full-DRIVE scope (which needs a Google
            // security assessment) and its "unverified app" consent warnings.
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                setOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = android.accounts.Account(email, "com.google")

            driveService = Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory(),
                credential
            )
                .setApplicationName("Luma Docs")
                .build()

            currentServiceEmail = email
            Log.d("GoogleDrive", "Service initialized successfully for $email")
            true
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error initializing service: ${e.message}", e)
            driveService = null
            currentServiceEmail = null
            false
        }
    }

    private fun handleAuthException(e: UserRecoverableAuthIOException) {
        Log.w("GoogleDrive", "User authorization required: ${e.message}")
        try {
            val intent: Intent = e.intent
            // Prefer the current Activity so the consent screen launches in-task; only fall
            // back to the app context (with NEW_TASK) if no Activity is currently resumed.
            val activity = LumaDocsApplication.currentActivity
            if (activity != null) {
                activity.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (ex: Exception) {
            Log.e("GoogleDrive", "Failed to start authorization activity: ${ex.message}", ex)
        }
    }

    override suspend fun uploadFile(
        fileName: String,
        mimeType: String,
        fileContent: ByteArray,
        parentFolderId: String?,
        description: String?,
        category: String?,
        makeEncrypted: Boolean,
        expiryDate: String?,
    ): UploadResult = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!ensureServiceInitialized()) {
                return@withContext UploadResult(
                    success = false,
                    errorMessage = "Failed to initialize Google Drive service. Please sign in again."
                )
            }

            val fileMetadata = File().apply {
                name = fileName
                this.mimeType = mimeType
                if (parentFolderId != null) {
                    parents = listOf(parentFolderId)
                }
                if (description != null) {
                    this.description = description
                }

                val props = mutableMapOf<String, String>()
                if (category != null) props["category"] = category
                if (makeEncrypted) props["encrypted"] = "true"
                if (expiryDate != null) props["expiryDate"] = expiryDate
                if (props.isNotEmpty()) {
                    appProperties = props
                }
            }

            val fileContentWrapper =
                com.google.api.client.http.ByteArrayContent(mimeType, fileContent)

            val uploadedFile = driveService!!.files()
                .create(fileMetadata, fileContentWrapper)
                .setFields("id, name, webViewLink, createdTime, description, appProperties")
                .execute()

            Log.d("GoogleDrive", "File uploaded successfully: ${uploadedFile.id}")
            UploadResult(
                success = true,
                fileId = uploadedFile.id
            )
        } catch (e: UserRecoverableAuthIOException) {
            handleAuthException(e)
            UploadResult(
                success = false,
                errorMessage = "Please authorize the app in the popup dialog. Grant permission and tap 'Retry Upload' button."
            )
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error uploading file: ${e.message}", e)
            UploadResult(
                success = false,
                errorMessage = "Upload failed: ${e.message}"
            )
        }
    }

    override suspend fun createFolder(folderName: String, parentFolderId: String?): String? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (!ensureServiceInitialized()) {
                    Log.e("GoogleDrive", "Drive service not initialized")
                    return@withContext null
                }

                val folderMetadata = File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    if (parentFolderId != null) {
                        parents = listOf(parentFolderId)
                    }
                }

                val folder = driveService!!.files()
                    .create(folderMetadata)
                    .setFields("id, name")
                    .execute()

                Log.d("GoogleDrive", "Folder created successfully: ${folder.id}")
                folder.id
            } catch (e: UserRecoverableAuthIOException) {
                handleAuthException(e)
                null
            } catch (e: Exception) {
                Log.e("GoogleDrive", "Error creating folder: ${e.message}", e)
                null
            }
        }

    override suspend fun listFiles(folderId: String?, query: String): List<DriveFile> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (!ensureServiceInitialized()) {
                    Log.e("GoogleDrive", "Drive service not initialized")
                    return@withContext emptyList()
                }

                var queryStr = "trashed = false"
                if (folderId != null) {
                    queryStr += " and '$folderId' in parents"
                }
                if (query.isNotEmpty()) {
                    queryStr += " and $query"
                }

                val fileList = driveService!!.files()
                    .list()
                    .setQ(queryStr)
                    .setFields("files(id, name, mimeType, description, createdTime, modifiedTime, size, parents, thumbnailLink, webViewLink, webContentLink, appProperties)")
                    .execute()

                fileList.files?.map { file ->
                    DriveFile(
                        id = file.id ?: "",
                        name = file.name ?: "",
                        mimeType = file.mimeType ?: "",
                        description = file.description,
                        createdTime = file.createdTime?.toStringRfc3339(),
                        modifiedTime = file.modifiedTime?.toStringRfc3339(),
                        size = file.get("size")?.toString()?.toLong() ?: 0L,
                        parentId = file.parents?.firstOrNull(),
                        thumbnailLink = file.thumbnailLink,
                        webViewLink = file.webViewLink,
                        webContentLink = file.webContentLink,
                        category = file.appProperties?.get("category"),
                        encrypted = file.appProperties?.get("encrypted") == "true",
                        expiryDate = file.appProperties?.get("expiryDate"),
                    )
                } ?: emptyList()
            } catch (e: UserRecoverableAuthIOException) {
                handleAuthException(e)
                emptyList()
            } catch (e: Exception) {
                Log.e("GoogleDrive", "Error listing files: ${e.message}", e)
                emptyList()
            }
        }

    override suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!ensureServiceInitialized()) {
                Log.e("GoogleDrive", "Drive service not initialized")
                return@withContext false
            }

            driveService!!.files().delete(fileId).execute()
            Log.d("GoogleDrive", "File deleted successfully: $fileId")
            true
        } catch (e: UserRecoverableAuthIOException) {
            handleAuthException(e)
            false
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error deleting file: ${e.message}", e)
            false
        }
    }

    override suspend fun updateFileMetadata(
        fileId: String,
        name: String,
        description: String?,
        expiryDate: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!ensureServiceInitialized()) {
                Log.e("GoogleDrive", "Drive service not initialized")
                return@withContext false
            }

            val metadata = File().apply {
                this.name = name
                // Empty string clears the description on Drive when the user removed it.
                this.description = description ?: ""

                // appProperties is patched (merged): mapping a key to null deletes just that
                // property, leaving category/encrypted untouched.
                val props = HashMap<String, String?>()
                props["expiryDate"] = expiryDate
                @Suppress("UNCHECKED_CAST")
                appProperties = props as MutableMap<String, String>
            }

            driveService!!.files()
                .update(fileId, metadata)
                .setFields("id, name, description, appProperties")
                .execute()

            Log.d("GoogleDrive", "File metadata updated: $fileId")
            true
        } catch (e: UserRecoverableAuthIOException) {
            handleAuthException(e)
            false
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error updating file metadata: ${e.message}", e)
            false
        }
    }

    override suspend fun getFileContent(fileId: String, isEncrypted: Boolean): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!ensureServiceInitialized()) {
                Log.e("GoogleDrive", "Drive service not initialized")
                return@withContext null
            }

            val inputStream = driveService!!.files().get(fileId).executeMediaAsInputStream()
            val bytes = inputStream.use { it.readBytes() }
            Log.d("GoogleDrive", "Downloaded file content: id=$fileId size=${bytes.size} encrypted=$isEncrypted")
            if (isEncrypted) {
                try {
                    val decrypted = app.lumadocs.kmp.utils.SecurityUtils.decrypt(bytes)
                    Log.d("GoogleDrive", "Decrypted content for file: $fileId size=${decrypted.size}")
                    decrypted
                } catch (e: Exception) {
                    Log.e("GoogleDrive", "Failed to decrypt content: ${e.message}", e)
                    null
                }
            } else bytes
        } catch (e: UserRecoverableAuthIOException) {
            handleAuthException(e)
            null
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error downloading file content: ${e.message}", e)
            null
        }
    }
}
