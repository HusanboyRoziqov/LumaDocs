package app.lumadocs.kmp.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lumadocs.kmp.data.PendingUpload
import app.lumadocs.kmp.platform.deleteBlob
import app.lumadocs.kmp.platform.readBlob
import app.lumadocs.kmp.platform.writeBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Local, persistent queue of documents "uploaded" while offline. Metadata is a small JSON list in
 * DataStore; each item's bytes are a plain file on disk (see BlobStore — keeping megabytes out of
 * the DataStore preferences file so all other DataStore reads stay fast). Nothing is evicted —
 * items stay until they're pushed to Drive.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class PendingUploadStore(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true }
    private val metaKey = stringPreferencesKey("pending_uploads_v2")

    /** Reactive list of pending uploads (emits on every enqueue/remove). */
    val items: Flow<List<PendingUpload>> = dataStore.data.map { prefs ->
        prefs[metaKey]?.let { runCatching { json.decodeFromString<List<PendingUpload>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun current(): List<PendingUpload> = items.first()

    /** Queues a document offline; returns its generated id (also the blob file name). */
    suspend fun enqueue(
        name: String,
        mimeType: String,
        bytes: ByteArray,
        category: String? = null,
        expiryDate: String? = null,
        encrypted: Boolean = false,
    ): String? {
        val id = "pending_${kotlin.time.Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100000)}"
        val written = withContext(Dispatchers.Default) { writeBlob(id, bytes) }
        if (!written) return null
        val item = PendingUpload(id, name, mimeType, category, expiryDate, encrypted, bytes.size.toLong())
        runCatching {
            dataStore.edit { prefs ->
                val list = (prefs[metaKey]?.let { json.decodeFromString<List<PendingUpload>>(it) } ?: emptyList()) + item
                prefs[metaKey] = json.encodeToString(list)
            }
        }
        return id
    }

    suspend fun bytesFor(id: String): ByteArray? = withContext(Dispatchers.Default) { readBlob(id) }

    suspend fun remove(id: String) {
        withContext(Dispatchers.Default) { deleteBlob(id) }
        runCatching {
            dataStore.edit { prefs ->
                val list = (prefs[metaKey]?.let { json.decodeFromString<List<PendingUpload>>(it) } ?: emptyList())
                    .filterNot { it.id == id }
                prefs[metaKey] = json.encodeToString(list)
            }
        }
    }

    /**
     * One-time cleanup of the old v1 storage that kept Base64 bytes inside DataStore (it made the
     * preferences file huge and slowed every DataStore read).
     */
    suspend fun purgeLegacy() {
        runCatching {
            dataStore.edit { prefs ->
                prefs.asMap().keys
                    .filter { it.name == "pending_uploads_v1" || it.name.startsWith("pending_bytes_") }
                    .forEach { prefs.remove(it) }
            }
        }
    }
}
