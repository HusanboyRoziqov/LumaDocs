package app.lumadocs.kmp.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.CurrentUserState
import app.lumadocs.kmp.data.PendingUpload
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.scheduleExpiryNotifications
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.utils.ErrorMessages
import app.lumadocs.kmp.utils.PendingUploadStore
import app.lumadocs.kmp.utils.PreviewCache
import app.lumadocs.kmp.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_load_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class DocumentsUiState(
    val isLoading: Boolean = false,
    val files: List<DriveFile>? = null, // display list: standalone files + one representative per folder
    val errorMessage: String? = null,
    val totalFiles: Int = 0,
    val isRefreshing: Boolean = false,
    val refreshOffline: Boolean = false, // user tried to refresh with no internet
    val folderContents: Map<String, List<DriveFile>> = emptyMap(), // folderId -> its files
    val folderNames: Map<String, String> = emptyMap(),             // folderId -> folder name
)

@Serializable
private data class GroupedFiles(
    val display: List<DriveFile>,
    val folderContents: Map<String, List<DriveFile>>,
    val folderNames: Map<String, String>,
)

class DocumentsViewModel : ViewModel(), KoinComponent {

    private val googleDriveRepository: GoogleDriveRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()
    private val authenticator: Authenticator by inject()
    private val pendingStore: PendingUploadStore by inject()

    /** Documents queued offline, waiting to be pushed to Drive. */
    val pendingUploads: Flow<List<PendingUpload>> = pendingStore.items

    private val _isUploadingPending = MutableStateFlow(false)
    val isUploadingPending: StateFlow<Boolean> = _isUploadingPending

    /** Reads a queued upload's bytes from the local cache (for showing its image in the list). */
    suspend fun pendingBytes(id: String): ByteArray? = pendingStore.bytesFor(id)

    /** No signed-in Google account → no Drive documents to show. */
    private fun isSignedIn(): Boolean = authenticator.getCurrentUser() != null

    /** Clears the in-memory list and the on-disk cache (used when signed out). */
    private suspend fun clearListAndCache() {
        runCatching { dataStore.edit { it.remove(cacheKey) } }
        _uiState.update {
            it.copy(
                isLoading = false, isRefreshing = false, refreshOffline = false,
                files = emptyList(), totalFiles = 0, errorMessage = null,
                folderContents = emptyMap(), folderNames = emptyMap(),
            )
        }
    }

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState

    private val _previewBytes = MutableStateFlow<ByteArray?>(null)
    val previewBytes: StateFlow<ByteArray?> = _previewBytes

    private val _thumbnails = MutableStateFlow<Map<String, ByteArray?>>(emptyMap())
    val thumbnails: StateFlow<Map<String, ByteArray?>> = _thumbnails

    private val recentSearchesKey = stringPreferencesKey("recent_searches_v1")
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    /** The user's own recent search terms (most-recent first), persisted across launches. */
    val recentSearches: StateFlow<List<String>> = _recentSearches

    private val query =
        "mimeType contains 'image/' or mimeType='application/pdf' or mimeType='application/vnd.openxmlformats-officedocument'"

    /** Newest prefetch pass wins — a refresh shouldn't race an in-flight one. */
    private var prefetchJob: Job? = null

    /** Cap the warm-up at the cache's LRU capacity so a pass can't evict its own earlier writes. */
    private val MAX_PREFETCH = 60

    private val cacheKey = stringPreferencesKey("docs_cache_v1")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        observeAuthAndLoad()
        loadRecentSearches()
        // Drop the old storage that kept Base64 bytes inside DataStore (perf).
        viewModelScope.launch {
            pendingStore.purgeLegacy()
            PreviewCache.purgeLegacy(dataStore)
        }
    }

    /**
     * Reacts to sign-in/out changes via the app-wide [CurrentUserState] flow so every screen that
     * shares this ViewModel (Vault, Reminders, Search) updates the moment the account connects or
     * disconnects — no manual refresh needed.
     */
    private fun observeAuthAndLoad() {
        viewModelScope.launch {
            var prevSignedIn: Boolean? = null
            CurrentUserState.user.collect { user ->
                val signedIn = !user?.userEmail.isNullOrEmpty()
                if (signedIn == prevSignedIn) return@collect
                prevSignedIn = signedIn
                if (signedIn) loadCacheThenFetchIfEmpty() else clearListAndCache()
            }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            val stored = runCatching {
                dataStore.data.first()[recentSearchesKey]?.let { json.decodeFromString<List<String>>(it) }
            }.getOrNull()
            if (stored != null) _recentSearches.value = stored
        }
    }

    /** Records a search term the user actually typed, most-recent first, de-duplicated, capped at 8. */
    fun addRecentSearch(query: String) {
        val term = query.trim()
        if (term.isEmpty()) return
        val updated = (listOf(term) + _recentSearches.value.filterNot { it.equals(term, ignoreCase = true) }).take(8)
        _recentSearches.value = updated
        viewModelScope.launch {
            runCatching { dataStore.edit { it[recentSearchesKey] = json.encodeToString(updated) } }
        }
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        viewModelScope.launch { runCatching { dataStore.edit { it.remove(recentSearchesKey) } } }
    }

    /** Offline-first: show the cached list immediately; only hit the server if there's no cache. */
    private fun loadCacheThenFetchIfEmpty() {
        viewModelScope.launch {
            // Signed out / guest: never surface a previous account's cached documents.
            if (!isSignedIn()) {
                clearListAndCache()
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val cached = runCatching {
                dataStore.data.first()[cacheKey]?.let { json.decodeFromString<GroupedFiles>(it) }
            }.getOrNull()

            if (cached != null && cached.display.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = cached.display,
                        totalFiles = cached.display.size,
                        folderContents = cached.folderContents,
                        folderNames = cached.folderNames,
                    )
                }
                prefetchImagePreviews(cached)
            } else if (isOnline()) {
                fetchAllFiles()
            } else {
                _uiState.update { it.copy(isLoading = false, files = emptyList(), totalFiles = 0) }
            }
        }
    }

    private suspend fun saveCache(grouped: GroupedFiles) {
        runCatching {
            dataStore.edit { it[cacheKey] = json.encodeToString(grouped) }
        }
    }

    /**
     * Warms the on-disk cache with the real bytes of every document — including the pages inside
     * folders, which the display list only represents by their first file. Once cached, list
     * thumbnails and the detail pager render from disk at full quality and keep working offline,
     * instead of re-fetching Drive's low-res (and short-lived) thumbnail links.
     *
     * Files larger than the cache's per-file ceiling are skipped up front, using the size Drive
     * already reported, so we never download something we'd only throw away.
     */
    private fun prefetchImagePreviews(grouped: GroupedFiles) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            if (!isOnline()) return@launch
            val targets = (grouped.display + grouped.folderContents.values.flatten())
                .distinctBy { it.id }
                .filter { (it.size ?: 0L) <= PreviewCache.MAX_BYTES }
                .take(MAX_PREFETCH)
            for (f in targets) {
                // Cheap existence check — don't read megabytes back just to test for a hit.
                if (withContext(Dispatchers.Default) { PreviewCache.localPath(f.id) } != null) continue
                val bytes = runCatching {
                    googleDriveRepository.getFileContent(f.id, isEncrypted = f.encrypted)
                }.getOrNull()
                if (bytes != null) PreviewCache.put(dataStore, f.id, bytes)
            }
        }
    }

    private suspend fun getRootFolderId(): String? =
        googleDriveRepository.listFiles(
            folderId = null,
            query = "name = 'Luma Docs' and mimeType = 'application/vnd.google-apps.folder'"
        ).firstOrNull()?.id

    /** Fetches files and groups those living inside user-created sub-folders under one representative. */
    private suspend fun loadGroupedFiles(): GroupedFiles {
        val rootId = getRootFolderId()
        val subFolders = if (rootId != null) {
            googleDriveRepository.listFiles(
                folderId = rootId,
                query = "mimeType = 'application/vnd.google-apps.folder'"
            )
        } else emptyList()
        val folderNames = subFolders.associate { it.id to it.name }
        val subIds = folderNames.keys

        val allFiles = googleDriveRepository.listFiles(folderId = null, query = query)

        val folderContents = allFiles
            .filter { it.parentId != null && it.parentId in subIds }
            .groupBy { it.parentId!! }
            .mapValues { (_, list) -> list.sortedBy { it.name } }

        val standalone = allFiles.filter { it.parentId == null || it.parentId !in subIds }
        // Represent each non-empty folder by its first file, then append standalone files.
        val reps = folderContents.values.mapNotNull { it.firstOrNull() }
        val display = reps + standalone

        return GroupedFiles(display, folderContents, folderNames)
    }

    fun fetchAllFiles() {
        viewModelScope.launch {
            if (!isSignedIn()) {
                clearListAndCache()
                return@launch
            }
            // Offline: keep whatever is cached (plus any pending items) — hitting Drive now could
            // come back empty and overwrite the cache, wiping the synced list from the screen.
            if (!isOnline()) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                return@launch
            }
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val grouped = loadGroupedFiles()
                saveCache(grouped)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = grouped.display,
                        totalFiles = grouped.display.size,
                        errorMessage = if (grouped.display.isEmpty() && it.errorMessage != null) it.errorMessage else null,
                        folderContents = grouped.folderContents,
                        folderNames = grouped.folderNames,
                    )
                }
                prefetchImagePreviews(grouped)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = it.files ?: emptyList(),
                        errorMessage = ErrorMessages.forOperation(Res.string.error_load_failed)
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // Signed out: refreshing should empty the list, not re-show stale cache.
            if (!isSignedIn()) {
                clearListAndCache()
                return@launch
            }
            if (!isOnline()) {
                _uiState.update { it.copy(isRefreshing = false, refreshOffline = true) }
                return@launch
            }
            _uiState.update { it.copy(isRefreshing = true, refreshOffline = false) }
            try {
                val grouped = loadGroupedFiles()
                saveCache(grouped)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        refreshOffline = false,
                        files = grouped.display,
                        totalFiles = grouped.display.size,
                        errorMessage = null,
                        folderContents = grouped.folderContents,
                        folderNames = grouped.folderNames,
                    )
                }
                prefetchImagePreviews(grouped)
                // A manual refresh while online also pushes any offline-queued documents.
                if (pendingStore.current().isNotEmpty()) uploadPending()
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep the cached list on a failed refresh instead of clearing it.
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = ErrorMessages.forOperation(Res.string.error_load_failed)
                    )
                }
            }
        }
    }

    /** Dismisses the offline banner (e.g. after the user acknowledges it). */
    fun dismissRefreshOffline() {
        _uiState.update { it.copy(refreshOffline = false) }
    }

    fun refreshFiles() = fetchAllFiles()

    /** Pushes all offline-queued documents to Drive, removing each from the queue on success. */
    fun uploadPending() {
        viewModelScope.launch {
            if (_isUploadingPending.value) return@launch
            if (!isOnline() || !isSignedIn()) return@launch
            _isUploadingPending.value = true
            try {
                val rootId = getRootFolderId() ?: googleDriveRepository.createFolder("Luma Docs", null)
                for (item in pendingStore.current()) {
                    val bytes = pendingStore.bytesFor(item.id) ?: continue
                    val content = if (item.encrypted) SecurityUtils.encrypt(bytes) else bytes
                    val result = googleDriveRepository.uploadFile(
                        fileName = item.name,
                        mimeType = item.mimeType,
                        fileContent = content,
                        parentFolderId = rootId,
                        description = null,
                        category = item.category,
                        makeEncrypted = item.encrypted,
                        expiryDate = item.expiryDate,
                    )
                    if (result.success) {
                        pendingStore.remove(item.id)
                        if (item.expiryDate != null && result.fileId != null) {
                            scheduleExpiryNotifications(result.fileId, item.name, item.expiryDate)
                        }
                    }
                }
                fetchAllFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUploadingPending.value = false
            }
        }
    }

    fun loadPreview(fileId: String) {
        viewModelScope.launch {
            try {
                _previewBytes.value = null
                val file = _uiState.value.files?.firstOrNull { it.id == fileId }
                val bytes = googleDriveRepository.getFileContent(fileId, isEncrypted = file?.encrypted ?: false)
                _previewBytes.value = bytes
            } catch (e: Exception) {
                _previewBytes.value = null
            }
        }
    }

    fun loadThumbnail(fileId: String) {
        viewModelScope.launch {
            try {
                val file = _uiState.value.files?.firstOrNull { it.id == fileId }
                val bytes = googleDriveRepository.getFileContent(fileId, isEncrypted = file?.encrypted ?: false)
                _thumbnails.update { it + (fileId to bytes) }
            } catch (e: Exception) {

            }
        }
    }

    fun getImageFiles(): List<DriveFile>? =
        _uiState.value.files?.filter { it.mimeType.contains("image", ignoreCase = true) }

    fun getDocumentFiles(): List<DriveFile>? =
        _uiState.value.files?.filter {
            it.mimeType.contains("pdf", ignoreCase = true) ||
                    it.mimeType.contains("document", ignoreCase = true) ||
                    it.mimeType.contains("word", ignoreCase = true)
        }

    fun updateFileMetadata(fileId: String, newName: String, newDescription: String?) {
        viewModelScope.launch {
            _uiState.update { state ->
                val updated = state.files?.map { f ->
                    if (f.id == fileId) f.copy(name = newName, description = newDescription)
                    else f
                }
                state.copy(files = updated)
            }
        }
    }

    /** Replaces a file in the in-memory list after its metadata was persisted to Drive. */
    fun updateFileInList(updated: DriveFile) {
        viewModelScope.launch {
            val newState = _uiState.updateAndGet { state ->
                val files = state.files?.map { if (it.id == updated.id) updated else it }
                val folderContents = state.folderContents.mapValues { (_, list) ->
                    list.map { if (it.id == updated.id) updated else it }
                }
                state.copy(files = files, folderContents = folderContents)
            }
            // Persist to the offline cache too, otherwise the edit (name/description/expiry) is
            // lost on the next app launch, which reads the stale cached list.
            saveCache(
                GroupedFiles(
                    display = newState.files ?: emptyList(),
                    folderContents = newState.folderContents,
                    folderNames = newState.folderNames,
                )
            )
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch {
            try {
                val success = googleDriveRepository.deleteFile(fileId)
                if (success) {
                    _uiState.update { state ->
                        val remaining = state.files?.filter { it.id != fileId }
                        state.copy(files = remaining, totalFiles = remaining?.size ?: 0)
                    }
                }
            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }
}
