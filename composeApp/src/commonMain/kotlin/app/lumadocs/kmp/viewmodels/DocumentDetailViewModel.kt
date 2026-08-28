package app.lumadocs.kmp.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.utils.ErrorMessages
import app.lumadocs.kmp.utils.PreviewCache
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_delete_failed
import lumadocs.composeapp.generated.resources.error_preview_failed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DocumentDetailViewModel : ViewModel(), KoinComponent {

    private val googleDriveRepository: GoogleDriveRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

    private val _file = MutableStateFlow<DriveFile?>(null)
    val file: StateFlow<DriveFile?> = _file

    private val _previewBytes = MutableStateFlow<ByteArray?>(null)
    val previewBytes: StateFlow<ByteArray?> = _previewBytes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * fileId -> on-disk path of its cached full-quality bytes. The detail pager renders from these
     * so every page shows the real photo (not Drive's low-res thumbnail) and keeps working offline.
     */
    private val _localPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    val localPaths: StateFlow<Map<String, String>> = _localPaths

    private val inFlight = mutableSetOf<String>()

    fun setFile(driveFile: DriveFile) {
        _file.value = driveFile
        _previewBytes.value = null
        _errorMessage.value = null
    }

    fun loadPreview(force: Boolean = false) {
        val fileId = _file.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (force) {
                    // Refresh: drop the cached copy and re-download from Drive.
                    PreviewCache.remove(dataStore, fileId)
                } else {
                    // Serve from the disk (DataStore) cache if we already have this file.
                    val cached = PreviewCache.get(dataStore, fileId)
                    if (cached != null) {
                        _previewBytes.value = cached
                        return@launch
                    }
                }
                _previewBytes.value = null
                val isEncrypted = _file.value?.encrypted ?: false
                val bytes = googleDriveRepository.getFileContent(fileId, isEncrypted = isEncrypted)
                if (bytes != null) PreviewCache.put(dataStore, fileId, bytes)
                _previewBytes.value = bytes
            } catch (e: Throwable) {
                e.printStackTrace()
                _errorMessage.value = ErrorMessages.forOperation(Res.string.error_preview_failed)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Persists metadata to Drive and, on success, updates the in-memory file so the UI reflects
     * the change immediately. Returns true when the server update succeeded.
     */
    suspend fun updateMetadata(
        newName: String,
        newDescription: String?,
        newExpiryDate: String?,
    ): Boolean {
        val fileId = _file.value?.id ?: return false
        _isSaving.value = true
        return try {
            val success = googleDriveRepository.updateFileMetadata(
                fileId = fileId,
                name = newName,
                description = newDescription,
                expiryDate = newExpiryDate,
            )
            if (success) {
                _file.update { current ->
                    current?.copy(
                        name = newName,
                        description = newDescription,
                        expiryDate = newExpiryDate,
                    )
                }
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _isSaving.value = false
        }
    }

    suspend fun deleteCurrentFile(): Boolean {
        val fileId = _file.value?.id ?: return false
        _isDeleting.value = true
        return try {
            val success = googleDriveRepository.deleteFile(fileId)
            if (success) {
                PreviewCache.remove(dataStore, fileId)
                _localPaths.update { it - fileId }
                _file.value = null
                _previewBytes.value = null
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = ErrorMessages.forOperation(Res.string.error_delete_failed)
            false
        } finally {
            _isDeleting.value = false
        }
    }

    fun getShareLink(): String? {
        val f = _file.value ?: return null
        return f.webContentLink ?: f.webViewLink
    }

    /**
     * Makes sure every [pages] entry has its full-quality bytes on disk, then publishes the paths
     * so the pager can swap the blurry thumbnail for the real image. Already-cached pages resolve
     * without touching the network; pages are fetched one by one so a folder scan doesn't fire a
     * burst of parallel downloads.
     */
    fun ensureCached(pages: List<DriveFile>) {
        val wanted = pages
            // A whole file is read into memory to cache it, so anything above the cache ceiling is
            // left to its Drive thumbnail rather than risking an OutOfMemoryError for a preview.
            .filter { (it.size ?: Long.MAX_VALUE) <= PreviewCache.MAX_BYTES }
            .filter { it.id !in _localPaths.value && inFlight.add(it.id) }
        if (wanted.isEmpty()) return
        viewModelScope.launch {
            for (page in wanted) {
                try {
                    val cached = withContext(Dispatchers.Default) { PreviewCache.localPath(page.id) }
                    val path = cached ?: run {
                        val bytes = googleDriveRepository.getFileContent(page.id, isEncrypted = page.encrypted)
                        if (bytes != null) PreviewCache.put(dataStore, page.id, bytes)
                        withContext(Dispatchers.Default) { PreviewCache.localPath(page.id) }
                    }
                    if (path != null) _localPaths.update { it + (page.id to path) }
                } catch (e: Throwable) {
                    // Includes OutOfMemoryError: a preview that doesn't fit must degrade to the
                    // thumbnail, never take the app down.
                    e.printStackTrace()
                } finally {
                    inFlight.remove(page.id)
                }
            }
        }
    }

    /** Decrypted content bytes for any file (e.g. a specific page in a folder), cached on disk. */
    suspend fun contentFor(target: DriveFile): ByteArray? {
        PreviewCache.get(dataStore, target.id)?.let { return it }
        return try {
            val bytes = googleDriveRepository.getFileContent(target.id, isEncrypted = target.encrypted)
            if (bytes != null) PreviewCache.put(dataStore, target.id, bytes)
            bytes
        } catch (e: Throwable) {
            // A file too large to buffer reports as "couldn't open" instead of crashing.
            e.printStackTrace()
            null
        }
    }
}
