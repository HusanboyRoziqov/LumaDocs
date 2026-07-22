package app.lumadocs.kmp.platform

import app.lumadocs.kmp.utils.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSSortDescriptor
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.Photos.PHImageRequestOptionsVersionCurrent
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewContentMode
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * PHAsset localIdentifiers contain slashes ("ABC-123/L0/001"), which would be parsed as path
 * segments in a URI. They're escaped so the whole identifier survives as the authority and
 * can be recovered verbatim.
 */
internal const val PH_ASSET_SCHEME = "phasset"

internal fun phAssetUri(localIdentifier: String): String =
    "$PH_ASSET_SCHEME://${localIdentifier.replace("/", "%2F")}"

internal fun localIdentifierFrom(uri: String): String? {
    if (!uri.startsWith("$PH_ASSET_SCHEME://")) return null
    return uri.removePrefix("$PH_ASSET_SCHEME://").replace("%2F", "/")
}

@OptIn(ExperimentalForeignApi::class)
internal fun assetFor(localIdentifier: String): PHAsset? {
    val result = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(localIdentifier), null)
    return result.firstObject() as? PHAsset
}

internal fun newestImageAssets(limit: Int): List<PHAsset> {
    val options = PHFetchOptions().apply {
        sortDescriptors = listOf(
            NSSortDescriptor.sortDescriptorWithKey("creationDate", ascending = false),
        )
    }
    val result = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)
    val count = min(result.count.toInt(), limit)
    return (0 until count).mapNotNull { result.objectAtIndex(it.toULong()) as? PHAsset }
}

private fun defaultRequestOptions() = PHImageRequestOptions().apply {
    // Photos stored only in iCloud would otherwise fail to load.
    setNetworkAccessAllowed(true)
    setSynchronous(false)
    setVersion(PHImageRequestOptionsVersionCurrent)
    setDeliveryMode(PHImageRequestOptionsDeliveryModeHighQualityFormat)
    setResizeMode(PHImageRequestOptionsResizeModeExact)
}

/** Loads a display-sized thumbnail for [asset], encoded as JPEG. */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun loadAssetThumbnail(asset: PHAsset, size: Int): ByteArray? =
    suspendCancellableCoroutine { cont ->
        PHImageManager.defaultManager().requestImageForAsset(
            asset = asset,
            targetSize = CGSizeMake(size.toDouble(), size.toDouble()),
            contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill.value,
            options = defaultRequestOptions(),
        ) { image, info ->
            // Photos may deliver a low-res placeholder first; only resume once.
            if (cont.isActive) {
                cont.resume(image?.let { UIImageJPEGRepresentation(it, 0.8)?.toByteArray() })
            }
        }
    }

/** Loads the full-resolution image data for [asset]. */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun loadAssetImage(asset: PHAsset): UIImage? =
    suspendCancellableCoroutine { cont ->
        PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(
            asset = asset,
            options = defaultRequestOptions(),
        ) { data: NSData?, _, _, _ ->
            if (cont.isActive) {
                cont.resume(data?.let { UIImage.imageWithData(it) })
            }
        }
    }

/** Scales [image] to fit [maxDimension] (never upscaling) and encodes it as JPEG. */
@OptIn(ExperimentalForeignApi::class)
internal fun encodeJpeg(image: UIImage, maxDimension: Double, quality: Int): ByteArray? {
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null

    val scale = min(1.0, min(maxDimension / width, maxDimension / height))
    val targetWidth = width * scale
    val targetHeight = height * scale

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val scaled = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return UIImageJPEGRepresentation(scaled ?: image, quality / 100.0)?.toByteArray()
}
