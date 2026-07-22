package app.lumadocs.kmp.platform

import app.lumadocs.kmp.utils.toByteArray
import app.lumadocs.kmp.utils.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

private fun blobDir(): String? {
    val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: return null
    val dir = "$docs/blobs"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    return dir
}

actual fun writeBlob(name: String, bytes: ByteArray): Boolean {
    val dir = blobDir() ?: return false
    return bytes.toNSData().writeToFile("$dir/$name", true)
}

actual fun readBlob(name: String): ByteArray? {
    val dir = blobDir() ?: return null
    return NSData.dataWithContentsOfFile("$dir/$name")?.toByteArray()
}

actual fun deleteBlob(name: String) {
    val dir = blobDir() ?: return
    NSFileManager.defaultManager.removeItemAtPath("$dir/$name", null)
}

actual fun blobPath(name: String): String? {
    val dir = blobDir() ?: return null
    val path = "$dir/$name"
    return if (NSFileManager.defaultManager.fileExistsAtPath(path)) path else null
}
