package app.lumadocs.kmp.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lumadocs.kmp.platform.blobPath
import app.lumadocs.kmp.platform.deleteBlob
import app.lumadocs.kmp.platform.readBlob
import app.lumadocs.kmp.platform.writeBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Disk-backed LRU cache of decrypted file preview bytes, keyed by file id, with a small in-memory
 * layer for speed. Lets the detail/documents screens open a file without re-downloading it — even
 * across app restarts.
 *
 * Bytes live as plain files (BlobStore) — NOT in DataStore: Base64 blobs in the preferences file
 * made every DataStore read parse megabytes and slowed the whole app down. DataStore only keeps
 * the tiny LRU order string.
 */
object PreviewCache {
    private const val MAX_ENTRIES = 60
    private const val MEMORY_ENTRIES = 4

    /** Files bigger than this are streamed on demand instead of being kept on disk. */
    const val MAX_BYTES = 15 * 1024 * 1024

    private val memory = LinkedHashMap<String, ByteArray>()
    private val orderKey = stringPreferencesKey("preview_order_v2")
    private fun blobName(id: String) = "preview_$id"

    /**
     * Absolute path of the cached full-quality bytes for [fileId], or null when not cached yet.
     * Lets image loaders decode straight off disk (downsampled, no full-size copy in memory)
     * instead of re-downloading a low-res Drive thumbnail.
     */
    fun localPath(fileId: String): String? = blobPath(blobName(fileId))

    suspend fun get(dataStore: DataStore<Preferences>, fileId: String): ByteArray? {
        memory[fileId]?.let { return it }
        val bytes = withContext(Dispatchers.Default) { readBlob(blobName(fileId)) } ?: return null
        putMemory(fileId, bytes)
        return bytes
    }

    suspend fun put(dataStore: DataStore<Preferences>, fileId: String, bytes: ByteArray) {
        putMemory(fileId, bytes)
        if (bytes.size > MAX_BYTES) return
        withContext(Dispatchers.Default) { writeBlob(blobName(fileId), bytes) }
        runCatching {
            dataStore.edit { prefs ->
                val order = (prefs[orderKey]?.split(",")?.filter { it.isNotBlank() && it != fileId }
                    ?: emptyList()) + fileId
                var trimmed = order
                while (trimmed.size > MAX_ENTRIES) {
                    deleteBlob(blobName(trimmed.first()))
                    trimmed = trimmed.drop(1)
                }
                prefs[orderKey] = trimmed.joinToString(",")
            }
        }
    }

    /** Wipes every cached preview (e.g. on sign out). */
    suspend fun clearAll(dataStore: DataStore<Preferences>) {
        memory.clear()
        runCatching {
            val order = dataStore.data.first()[orderKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            withContext(Dispatchers.Default) { order.forEach { deleteBlob(blobName(it)) } }
            dataStore.edit { prefs -> prefs.remove(orderKey) }
        }
    }

    suspend fun remove(dataStore: DataStore<Preferences>, fileId: String) {
        memory.remove(fileId)
        withContext(Dispatchers.Default) { deleteBlob(blobName(fileId)) }
        runCatching {
            dataStore.edit { prefs ->
                val order = prefs[orderKey]?.split(",")?.filter { it.isNotBlank() && it != fileId }
                if (order != null) prefs[orderKey] = order.joinToString(",")
            }
        }
    }

    /**
     * One-time cleanup of the old storage that kept Base64 preview bytes inside DataStore
     * (it bloated the preferences file and slowed every DataStore read).
     */
    suspend fun purgeLegacy(dataStore: DataStore<Preferences>) {
        runCatching {
            dataStore.edit { prefs ->
                prefs.asMap().keys
                    .filter { it.name.startsWith("preview_") && it.name != orderKey.name }
                    .forEach { prefs.remove(it) }
                prefs.remove(stringPreferencesKey("preview_order"))
            }
        }
    }

    private fun putMemory(fileId: String, bytes: ByteArray) {
        memory.remove(fileId)
        memory[fileId] = bytes
        while (memory.size > MEMORY_ENTRIES) {
            val oldest = memory.keys.firstOrNull() ?: break
            memory.remove(oldest)
        }
    }
}
