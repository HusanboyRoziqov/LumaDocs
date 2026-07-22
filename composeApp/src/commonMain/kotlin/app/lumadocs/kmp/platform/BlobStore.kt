package app.lumadocs.kmp.platform

/**
 * Tiny persistent blob store for large binary payloads (e.g. offline-queued photos).
 *
 * Blobs live as plain files in app storage — NEVER in DataStore: multi-MB Base64 strings in the
 * preferences file make every DataStore read (theme, locale, caches…) parse megabytes and slow the
 * whole app down.
 */
expect fun writeBlob(name: String, bytes: ByteArray): Boolean

expect fun readBlob(name: String): ByteArray?

expect fun deleteBlob(name: String)

/** Absolute path of a stored blob (for image loaders), or null if it doesn't exist. */
expect fun blobPath(name: String): String?
