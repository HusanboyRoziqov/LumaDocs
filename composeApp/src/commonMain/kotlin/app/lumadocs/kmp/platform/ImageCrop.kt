package app.lumadocs.kmp.platform

/**
 * Crops [bytes] to the given normalized rectangle (each value 0..1, relative to the image)
 * and returns the re-encoded image bytes, or null on failure.
 */
expect suspend fun cropImageBytes(
    bytes: ByteArray,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    mimeType: String,
): ByteArray?
