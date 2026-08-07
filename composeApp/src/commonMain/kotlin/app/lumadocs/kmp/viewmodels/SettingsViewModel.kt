package app.lumadocs.kmp.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.CurrentUserState
import app.lumadocs.kmp.SessionResetState
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data.Response
import app.lumadocs.kmp.platform.BackupFileMeta
import app.lumadocs.kmp.platform.buildAndShareBackup
import app.lumadocs.kmp.platform.importBackupFromUri
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.utils.EncryptionSettings
import app.lumadocs.kmp.utils.ErrorMessages
import app.lumadocs.kmp.utils.PreviewCache
import app.lumadocs.kmp.utils.SecurityUtils
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_backup_empty
import lumadocs.composeapp.generated.resources.error_backup_invalid
import lumadocs.composeapp.generated.resources.error_disconnect_failed
import lumadocs.composeapp.generated.resources.error_export_failed
import lumadocs.composeapp.generated.resources.error_folder_not_found
import lumadocs.composeapp.generated.resources.error_import_failed
import lumadocs.composeapp.generated.resources.error_load_failed
import lumadocs.composeapp.generated.resources.error_no_files_export
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authenticator: Authenticator,
    private val driveRepository: GoogleDriveRepository,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    private var pendingImport: List<Pair<BackupFileMeta, ByteArray>> = emptyList()

    /** Whether documents added from now on are encrypted before upload. */
    val encryptNewUploads = EncryptionSettings.flow(dataStore)
        .stateIn(viewModelScope, SharingStarted.Eagerly, EncryptionSettings.DEFAULT)

    fun setEncryptNewUploads(enabled: Boolean) {
        viewModelScope.launch { EncryptionSettings.set(dataStore, enabled) }
    }

    init {
        checkGoogleDriveConnection()
    }

    private fun checkGoogleDriveConnection() {
        val currentUser = authenticator.getCurrentUser()
        _uiState.update {
            it.copy(
                isGoogleDriveConnected = currentUser != null && !currentUser.userEmail.isNullOrEmpty(),
                currentUser = currentUser
            )
        }
    }

    fun onGoogleDriveToggle() {
        if (_uiState.value.isGoogleDriveConnected) {
            disconnectGoogleDrive()
        } else {
            connectGoogleDrive()
        }
    }

    private fun connectGoogleDrive() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val response = authenticator.login()) {
                is Response.Failure -> {
                    val message = ErrorMessages.forAuthCode(response.error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = message,
                            isGoogleDriveConnected = false
                        )
                    }
                }

                is Response.Success<*> -> {
                    val user = response.data as FirebaseUser
                    CurrentUserState.set(user)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isGoogleDriveConnected = true,
                            currentUser = user,
                            error = null
                        )
                    }
                }

                is Response.Loading -> {
                    _uiState.update { it.copy(isLoading = response.loading) }
                }
            }
        }
    }

    private fun disconnectGoogleDrive() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                authenticator.logOut()
                clearCachedData()
                CurrentUserState.clear()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGoogleDriveConnected = false,
                        currentUser = null,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = getString(Res.string.error_disconnect_failed))
                }
            }
        }
    }

    /**
     * Full wipe triggered from the connected-status tap: signs the user out and
     * clears every session + cached file. The app-lock PIN is cleared separately
     * by the caller (see [PinViewModel.disableLock]). On success it raises
     * [SessionResetState] so the app root sends the user back to onboarding.
     */
    fun clearAllSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                authenticator.logOut()
                clearCachedData()
                CurrentUserState.clear()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGoogleDriveConnected = false,
                        currentUser = null,
                        error = null
                    )
                }
                SessionResetState.trigger()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = getString(Res.string.error_disconnect_failed))
                }
            }
        }
    }

    /** Clears the on-disk file list, recent searches + preview caches (called on sign out). */
    private suspend fun clearCachedData() {
        runCatching {
            dataStore.edit {
                it.remove(stringPreferencesKey("docs_cache_v1"))
                it.remove(stringPreferencesKey("recent_searches_v1"))
            }
        }
        PreviewCache.clearAll(dataStore)
    }

    private suspend fun getLumaDocsFolderId(): String? {
        val folders = driveRepository.listFiles(
            folderId = null,
            query = "name = 'Luma Docs' and mimeType = 'application/vnd.google-apps.folder'"
        )
        return folders.firstOrNull()?.id
    }

    /** Step 1 of export: load the file list from Drive so the user can preview and pick. */
    fun prepareExport() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    backupPhase = BackupPhase.EXPORT_LOADING,
                    backupError = null,
                    exportProgress = 0f
                )
            }
            try {
                val folderId = getLumaDocsFolderId()
                if (folderId == null) {
                    _uiState.update {
                        it.copy(
                            backupPhase = BackupPhase.ERROR,
                            backupError = getString(Res.string.error_folder_not_found)
                        )
                    }
                    return@launch
                }
                val files = driveRepository.listFiles(folderId = folderId, query = "")
                if (files.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            backupPhase = BackupPhase.ERROR,
                            backupError = getString(Res.string.error_no_files_export)
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.EXPORT_PREVIEW,
                        exportPreviewFiles = files
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.ERROR,
                        backupError = ErrorMessages.forOperation(Res.string.error_load_failed)
                    )
                }
            }
        }
    }

    /** Step 2 of export: build and share a backup containing only the chosen files. */
    fun exportSelectedFiles(selectedIds: Set<String>) {
        viewModelScope.launch {
            val selected = _uiState.value.exportPreviewFiles.filter { it.id in selectedIds }
            if (selected.isEmpty()) return@launch
            _uiState.update {
                it.copy(
                    backupPhase = BackupPhase.EXPORTING,
                    backupError = null,
                    exportProgress = 0f
                )
            }
            try {
                val metas = selected.map { f ->
                    BackupFileMeta(
                        name = f.name,
                        mimeType = f.mimeType,
                        category = f.category,
                        description = f.description,
                        encrypted = f.encrypted,
                    )
                }

                buildAndShareBackup(metas) { index ->
                    val file = selected.getOrNull(index) ?: return@buildAndShareBackup null
                    _uiState.update { it.copy(exportProgress = index.toFloat() / selected.size) }
                    driveRepository.getFileContent(file.id, isEncrypted = false)
                }
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.IDLE,
                        exportProgress = 0f,
                        exportPreviewFiles = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.ERROR,
                        backupError = ErrorMessages.forOperation(Res.string.error_export_failed)
                    )
                }
            }
        }
    }

    /** Step 1 of import: read the whole backup into memory so the user can preview and pick. */
    fun prepareImport(uriString: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    backupPhase = BackupPhase.IMPORT_LOADING,
                    backupError = null,
                    importProgress = 0f
                )
            }
            try {
                val loaded = mutableListOf<Pair<BackupFileMeta, ByteArray>>()
                val ok = importBackupFromUri(uriString) { meta, bytes ->
                    loaded.add(meta to bytes)
                }
                when {
                    !ok -> _uiState.update {
                        it.copy(
                            backupPhase = BackupPhase.ERROR,
                            backupError = getString(Res.string.error_backup_invalid)
                        )
                    }

                    loaded.isEmpty() -> _uiState.update {
                        it.copy(
                            backupPhase = BackupPhase.ERROR,
                            backupError = getString(Res.string.error_backup_empty)
                        )
                    }

                    else -> {
                        pendingImport = loaded
                        _uiState.update {
                            it.copy(
                                backupPhase = BackupPhase.IMPORT_PREVIEW,
                                importPreviewFiles = loaded.map { p -> p.first })
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.ERROR,
                        backupError = ErrorMessages.forOperation(Res.string.error_import_failed)
                    )
                }
            }
        }
    }

    /** Step 2 of import: upload only the chosen files, skipping ones already on Drive. */
    fun importSelectedFiles(selectedIndices: Set<Int>) {
        viewModelScope.launch {
            val toImport = pendingImport.filterIndexed { index, _ -> index in selectedIndices }
            if (toImport.isEmpty()) return@launch
            _uiState.update {
                it.copy(
                    backupPhase = BackupPhase.IMPORTING,
                    backupError = null,
                    importProgress = 0f
                )
            }
            try {
                val folderId = getLumaDocsFolderId()

                val existingNames: Set<String> = if (folderId != null) {
                    driveRepository.listFiles(folderId = folderId, query = "").map { it.name }
                        .toHashSet()
                } else emptySet()

                var uploaded = 0
                var skipped = 0

                for ((meta, bytes) in toImport) {
                    if (meta.name in existingNames) {
                        skipped++
                        continue
                    }
                    driveRepository.uploadFile(
                        fileName = meta.name,
                        mimeType = meta.mimeType,
                        fileContent = bytes,
                        parentFolderId = folderId,
                        description = meta.description,
                        category = meta.category,
                        makeEncrypted = meta.encrypted,
                    )
                    uploaded++
                    _uiState.update { it.copy(importProgress = uploaded.toFloat()) }
                }

                pendingImport = emptyList()
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.SUCCESS,
                        importedCount = uploaded,
                        skippedCount = skipped,
                        importProgress = 1f,
                        importPreviewFiles = emptyList(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupPhase = BackupPhase.ERROR,
                        backupError = ErrorMessages.forOperation(Res.string.error_import_failed)
                    )
                }
            }
        }
    }

    /** Decrypted bytes of a file already loaded from the import backup, for previewing. */
    fun importFileBytes(index: Int): ByteArray? {
        val (meta, bytes) = pendingImport.getOrNull(index) ?: return null
        return if (meta.encrypted) {
            runCatching { SecurityUtils.decrypt(bytes) }.getOrNull()
        } else bytes
    }

    /** Downloads (and decrypts if needed) a Drive file's content so it can be previewed. */
    suspend fun downloadExportFile(fileId: String): ByteArray? {
        val file = _uiState.value.exportPreviewFiles.firstOrNull { it.id == fileId } ?: return null
        return runCatching {
            driveRepository.getFileContent(fileId, isEncrypted = file.encrypted)
        }.getOrNull()
    }

    /** Abort an in-progress export/import preview and return to idle. */
    fun cancelBackupFlow() {
        pendingImport = emptyList()
        _uiState.update {
            it.copy(
                backupPhase = BackupPhase.IDLE,
                backupError = null,
                exportPreviewFiles = emptyList(),
                importPreviewFiles = emptyList(),
                exportProgress = 0f,
                importProgress = 0f,
            )
        }
    }

    fun dismissBackupResult() {
        pendingImport = emptyList()
        _uiState.update {
            it.copy(
                backupPhase = BackupPhase.IDLE,
                backupError = null,
                importedCount = 0,
                skippedCount = 0,
                importProgress = 0f,
                exportProgress = 0f,
                exportPreviewFiles = emptyList(),
                importPreviewFiles = emptyList(),
            )
        }
    }
}

enum class BackupPhase {
    IDLE,
    EXPORT_LOADING, EXPORT_PREVIEW, EXPORTING,
    IMPORT_LOADING, IMPORT_PREVIEW, IMPORTING,
    SUCCESS, ERROR
}

data class SettingsState(
    val isLoading: Boolean = false,
    val isGoogleDriveConnected: Boolean = false,
    val currentUser: FirebaseUser? = null,
    val error: String? = null,
    val backupPhase: BackupPhase = BackupPhase.IDLE,
    val backupError: String? = null,
    val exportProgress: Float = 0f,
    val importProgress: Float = 0f,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val exportPreviewFiles: List<DriveFile> = emptyList(),
    val importPreviewFiles: List<BackupFileMeta> = emptyList(),
)
