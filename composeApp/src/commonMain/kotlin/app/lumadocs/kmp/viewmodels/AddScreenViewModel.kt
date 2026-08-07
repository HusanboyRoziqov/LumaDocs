package app.lumadocs.kmp.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.scheduleExpiryNotifications
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.utils.EncryptionSettings
import app.lumadocs.kmp.utils.ErrorMessages
import app.lumadocs.kmp.utils.PendingUploadStore
import app.lumadocs.kmp.utils.SecurityUtils
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_drive_access
import lumadocs.composeapp.generated.resources.error_folder_create_failed
import lumadocs.composeapp.generated.resources.error_no_files_selected
import lumadocs.composeapp.generated.resources.error_upload_failed
import lumadocs.composeapp.generated.resources.upload_success
import lumadocs.composeapp.generated.resources.upload_success_encrypted
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class SelectedFile(
    val fileName: String,
    val mimeType: String,
    val fileBytes: ByteArray,
    val description: String = "",
    val makeEncrypted: Boolean = false,
    val expiryDate: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SelectedFile
        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!fileBytes.contentEquals(other.fileBytes)) return false
        if (description != other.description) return false
        if (makeEncrypted != other.makeEncrypted) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileBytes.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + makeEncrypted.hashCode()
        return result
    }
}

data class AddScreenUiState(
    val isLoading: Boolean = false,
    val uploadProgress: Int = 0,
    /** Pages finished / total in the current upload, for the blocking progress dialog. */
    val uploadedCount: Int = 0,
    val totalCount: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val uploadedFileId: String? = null,
    val needsAuthorization: Boolean = false,
    val needsGoogleDriveConnection: Boolean = false,
    val selectedFiles: List<SelectedFile> = emptyList(),
    val createdFolderId: String? = null,
)

class AddScreenViewModel : ViewModel(), KoinComponent {

    private val googleDriveRepository: GoogleDriveRepository by inject()
    private val authenticator: Authenticator by inject()
    private val pendingStore: PendingUploadStore by inject()
    private val dataStore: DataStore<Preferences> by inject()

    /**
     * The Settings-owned default, kept live so a file picked right after the toggle flips gets the
     * new value. Only images are encrypted: ciphered PDFs and office files can't be handed to an
     * external viewer, which is the only way this app can open them.
     */
    private val encryptByDefault = EncryptionSettings.flow(dataStore)
        .stateIn(viewModelScope, SharingStarted.Eagerly, EncryptionSettings.DEFAULT)

    /** True when there is no signed-in Google account to talk to Drive. */
    private fun isSignedIn(): Boolean = authenticator.getCurrentUser() != null

    /** Dismiss the "connect Google Drive" prompt. */
    fun dismissConnectPrompt() {
        _uiState.value = _uiState.value.copy(needsGoogleDriveConnection = false)
    }

    private val _uiState = MutableStateFlow(AddScreenUiState())
    val uiState: StateFlow<AddScreenUiState> = _uiState

    private var pendingUpload: UploadDetails? = null

    private companion object {
        const val ROOT_FOLDER_NAME = "Luma Docs"
    }

    data class UploadDetails(
        val fileName: String,
        val mimeType: String,
        val fileBytes: ByteArray,
        val parentFolderId: String?,
        val category: String,
        val description: String? = null,
        val makeEncrypted: Boolean = false,
    )

    fun addFileToList(
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ) {
        val currentFiles = _uiState.value.selectedFiles.toMutableList()
        val encrypted = mimeType.startsWith("image/") && encryptByDefault.value
        currentFiles.add(SelectedFile(fileName, mimeType, fileBytes, makeEncrypted = encrypted))
        _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
    }

    /** The current Settings default — lets the add screen show its switch in the matching state. */
    fun encryptionDefault(): Boolean = encryptByDefault.value

    fun updateFileDescription(description: String) {
        val currentFiles = _uiState.value.selectedFiles.toMutableList()
        currentFiles.forEachIndexed { index, file ->
            currentFiles[index] = file.copy(description = description)
        }
        _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
    }

    fun setEncryptionForAll(makeEncrypted: Boolean) {
        val currentFiles = _uiState.value.selectedFiles.toMutableList()
        currentFiles.forEachIndexed { index, file ->
            currentFiles[index] = file.copy(makeEncrypted = makeEncrypted)
        }
        _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
    }

    fun removeFileFromList(index: Int) {
        val currentFiles = _uiState.value.selectedFiles.toMutableList()
        if (index in currentFiles.indices) {
            currentFiles.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedFiles = currentFiles)
        }
    }

    private suspend fun getOrCreateRootFolderId(): String? {
        return try {
            val folders = googleDriveRepository.listFiles(
                folderId = null,
                query = "name = '$ROOT_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder'"
            )
            if (folders.isNotEmpty()) {
                folders.first().id
            } else {
                googleDriveRepository.createFolder(ROOT_FOLDER_NAME, null)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun uploadMultipleFiles(
        title: String,
        folderName: String = "",
        expiryDate: String? = null,
    ) {
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    errorMessage = getString(Res.string.error_no_files_selected)
                )
            }
            return
        }

        viewModelScope.launch {
            // Offline: queue locally (cache) and show it in the vault; upload later from the banner.
            if (!isOnline()) {
                val multiple = files.size > 1
                files.forEachIndexed { index, file ->
                    val fileName = when {
                        multiple && title.isNotBlank() -> "$title ${index + 1}"
                        title.isNotBlank() -> title
                        else -> file.fileName
                    }
                    pendingStore.enqueue(
                        name = fileName,
                        mimeType = file.mimeType,
                        bytes = file.fileBytes,
                        expiryDate = expiryDate,
                        encrypted = file.makeEncrypted,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Saved offline — will upload when connected",
                    uploadProgress = 100,
                )
                return@launch
            }
            try {
                // No signed-in account → don't trigger the broken Drive consent; guide to Settings.
                if (!isSignedIn()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsGoogleDriveConnection = true
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null,
                    needsAuthorization = false,
                    needsGoogleDriveConnection = false
                )

                val rootFolderId = getOrCreateRootFolderId()
                if (rootFolderId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsGoogleDriveConnection = true
                    )
                    return@launch
                }

                // For multiple files, put them inside a user-named sub-folder.
                val multiple = files.size > 1
                val parentFolderId: String
                if (multiple && folderName.isNotBlank()) {
                    val subId = googleDriveRepository.createFolder(folderName.trim(), rootFolderId)
                    if (subId == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = getString(Res.string.error_folder_create_failed)
                        )
                        return@launch
                    }
                    parentFolderId = subId
                    _uiState.value = _uiState.value.copy(createdFolderId = subId)
                } else {
                    parentFolderId = rootFolderId
                }

                var successCount = 0
                var failCount = 0

                files.forEachIndexed { index, file ->

                    // Report which page is in flight so the blocking upload dialog can count up.
                    _uiState.value = _uiState.value.copy(
                        uploadedCount = index,
                        totalCount = files.size,
                        uploadProgress = (index * 100) / files.size,
                    )

                    val contentToUpload = if (file.makeEncrypted) SecurityUtils.encrypt(file.fileBytes) else file.fileBytes

                    val fileName = when {
                        multiple && title.isNotBlank() -> "$title ${index + 1}"
                        title.isNotBlank() -> title
                        else -> file.fileName
                    }

                    val result = googleDriveRepository.uploadFile(
                        fileName = fileName,
                        mimeType = file.mimeType,
                        fileContent = contentToUpload,
                        parentFolderId = parentFolderId,
                        description = file.description,
                        category = null,
                        makeEncrypted = file.makeEncrypted,
                        expiryDate = expiryDate
                    )

                    if (result.success) {
                        successCount++
                        _uiState.value = _uiState.value.copy(
                            uploadedCount = index + 1,
                            uploadProgress = ((index + 1) * 100) / files.size,
                        )

                        if (expiryDate != null && result.fileId != null) {
                            scheduleExpiryNotifications(result.fileId, fileName, expiryDate)
                        }
                    } else {
                        failCount++
                        if (result.errorMessage?.contains("authorize", ignoreCase = true) == true) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = getString(Res.string.error_drive_access),
                                needsAuthorization = true
                            )
                            return@launch
                        }
                    }
                }

                // Only say "encrypted" when the uploaded files were actually encrypted.
                val allEncrypted = files.isNotEmpty() && files.all { it.makeEncrypted }
                val successRes =
                    if (allEncrypted) Res.string.upload_success_encrypted else Res.string.upload_success
                if (failCount == 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = getString(successRes, successCount),
                        uploadProgress = 100
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = getString(successRes, successCount),
                        errorMessage = getString(Res.string.error_upload_failed)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ErrorMessages.forOperation(Res.string.error_upload_failed),
                    needsAuthorization = e.message?.contains("authorize", ignoreCase = true)
                        ?: false
                )
            }
        }
    }

    fun uploadFile(
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
        category: String,
        parentFolderId: String? = null,
        description: String? = null,
    ) {

        pendingUpload =
            UploadDetails(fileName, mimeType, fileBytes, parentFolderId, category, description)

        performUpload(fileName, mimeType, fileBytes, category, parentFolderId, description)
    }

    fun retryUpload() {
        pendingUpload?.let { details ->
            performUpload(
                details.fileName,
                details.mimeType,
                details.fileBytes,
                details.category,
                details.parentFolderId,
                details.description
            )
        }
    }

    private fun performUpload(
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
        category: String,
        parentFolderId: String? = null,
        description: String? = null,
    ) {
        viewModelScope.launch {
            try {
                if (!isSignedIn()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsGoogleDriveConnection = true
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    successMessage = null,
                    needsAuthorization = false,
                    needsGoogleDriveConnection = false
                )

                val actualParentId = parentFolderId ?: getOrCreateRootFolderId()
                if (actualParentId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsGoogleDriveConnection = true
                    )
                    return@launch
                }

                val contentToUpload = if (pendingUpload?.makeEncrypted == true) SecurityUtils.encrypt(fileBytes) else fileBytes

                val finalFileName = "${category}_$fileName"

                val result = googleDriveRepository.uploadFile(
                    fileName = finalFileName,
                    mimeType = mimeType,
                    fileContent = contentToUpload,
                    parentFolderId = actualParentId,
                    description = description,
                    makeEncrypted = pendingUpload?.makeEncrypted ?: false
                )

                if (result.success && result.fileId != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        uploadedFileId = result.fileId,
                        successMessage = getString(
                            if (pendingUpload?.makeEncrypted == true) Res.string.upload_success_encrypted
                            else Res.string.upload_success,
                            1
                        ),
                        uploadProgress = 100,
                        needsAuthorization = false
                    )

                    pendingUpload = null
                } else if (result.errorMessage?.contains("authorize", ignoreCase = true) == true) {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getString(Res.string.error_drive_access),
                        needsAuthorization = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getString(Res.string.error_upload_failed)
                    )
                    pendingUpload = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ErrorMessages.forOperation(Res.string.error_upload_failed),
                    needsAuthorization = e.message?.contains("authorize", ignoreCase = true)
                        ?: false
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null,
            needsAuthorization = false
        )
    }

    fun clearSelectedFiles() {
        _uiState.value = _uiState.value.copy(
            selectedFiles = emptyList(),
            createdFolderId = null
        )
    }

    fun resetState() {
        _uiState.value = AddScreenUiState()
        pendingUpload = null
    }
}
