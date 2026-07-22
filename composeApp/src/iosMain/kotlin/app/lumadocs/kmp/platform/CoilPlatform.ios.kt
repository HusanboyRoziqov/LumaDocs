package app.lumadocs.kmp.platform

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.FileSystem

private const val THUMBNAIL_SIZE = 480

/**
 * Resolves the `phasset://` URIs from [loadGalleryImages] by pulling thumbnails out of the
 * photo library, so the gallery grid can keep using a plain `AsyncImage(model = uri)` in
 * common code. Loading stays lazy — only the cells Coil actually requests are decoded.
 */
internal class PhAssetFetcher(
    private val localIdentifier: String,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val asset = assetFor(localIdentifier) ?: return null
        val bytes = loadAssetThumbnail(asset, THUMBNAIL_SIZE) ?: return null
        return SourceFetchResult(
            source = ImageSource(Buffer().apply { write(bytes) }, FileSystem.SYSTEM),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val identifier = localIdentifierFrom(data.toString()) ?: return null
            return PhAssetFetcher(identifier)
        }
    }
}

actual fun ComponentRegistry.Builder.addPlatformComponents(): ComponentRegistry.Builder =
    add(PhAssetFetcher.Factory())
