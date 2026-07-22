package app.lumadocs.kmp.platform

import app.lumadocs.kmp.LumaDocsApplication
import java.io.File

private fun blobDir(): File =
    File(LumaDocsApplication.instance.filesDir, "blobs").apply { if (!exists()) mkdirs() }

actual fun writeBlob(name: String, bytes: ByteArray): Boolean = runCatching {
    File(blobDir(), name).writeBytes(bytes)
    true
}.getOrDefault(false)

actual fun readBlob(name: String): ByteArray? = runCatching {
    File(blobDir(), name).takeIf { it.exists() }?.readBytes()
}.getOrNull()

actual fun deleteBlob(name: String) {
    runCatching { File(blobDir(), name).delete() }
}

actual fun blobPath(name: String): String? =
    File(blobDir(), name).takeIf { it.exists() }?.absolutePath
