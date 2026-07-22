package app.lumadocs.kmp.utils

expect object ImageOptimizer {

    fun compressForThumbnail(
        imageBytes: ByteArray,
        targetSizeKB: Int = 350,
        maxWidth: Int = 480,
        maxHeight: Int = 480,
        startQuality: Int = 75
    ): ByteArray

    fun getOptimizedThumbnailUrl(
        webViewLink: String?,
        width: Int = 480,
        height: Int = 480
    ): String?
}
