package app.lumadocs.kmp.platform

/**
 * Opens a file (given its raw bytes) in an external viewer/app.
 * Used to preview documents that aren't images from the backup preview screen.
 */
expect fun openFileExternally(bytes: ByteArray, fileName: String, mimeType: String): Boolean
