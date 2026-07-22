package app.lumadocs.kmp.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import app.lumadocs.kmp.LumaDocsApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual suspend fun buildAndShareBackup(
    metas: List<BackupFileMeta>,
    fetchFileBytes: suspend (index: Int) -> ByteArray?,
) {
    val context = LumaDocsApplication.instance
    val zipFile = File(context.cacheDir, "lumadocs_backup.lumadocsbackup")

    withContext(Dispatchers.IO) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(buildManifestJson(metas).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            metas.indices.forEach { index ->
                val bytes = fetchFileBytes(index) ?: return@forEach
                zip.putNextEntry(ZipEntry("files/$index.bin"))
                bytes.inputStream().copyTo(zip)
                zip.closeEntry()
            }
        }
    }

    val uri = FileProvider.getUriForFile(context, "app.lumadocs.kmp.fileprovider", zipFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "application/x-lumadocsbackup"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Share backup")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

actual suspend fun importBackupFromUri(
    uriString: String,
    onFileFound: suspend (meta: BackupFileMeta, bytes: ByteArray) -> Unit,
): Boolean {
    val context = LumaDocsApplication.instance
    val uri = Uri.parse(uriString)

    return withContext(Dispatchers.IO) {
        try {

            var metas: List<BackupFileMeta>? = null
            val fileBytes = mutableMapOf<Int, ByteArray>()

            val stream = if (uriString.startsWith("/")) FileInputStream(File(uriString))
                         else context.contentResolver.openInputStream(uri)

            stream?.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "manifest.json" ->
                                metas = parseManifestJson(zip.readBytes().toString(Charsets.UTF_8))
                            entry.name.startsWith("files/") -> {
                                val idx = entry.name.removePrefix("files/").removeSuffix(".bin").toIntOrNull()
                                if (idx != null) fileBytes[idx] = zip.readBytes()
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            val resolvedMetas = metas ?: return@withContext false

            resolvedMetas.forEachIndexed { index, meta ->
                val bytes = fileBytes[index] ?: return@forEachIndexed
                onFileFound(meta, bytes)
                fileBytes.remove(index)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private fun buildManifestJson(metas: List<BackupFileMeta>): String {
    val items = metas.joinToString(",") { m ->
        """{"name":${js(m.name)},"mimeType":${js(m.mimeType)},"category":${jsn(m.category)},"description":${jsn(m.description)},"encrypted":${m.encrypted}}"""
    }
    return "[$items]"
}

private fun parseManifestJson(json: String): List<BackupFileMeta>? {
    return try {
        val inner = json.trim().removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) return emptyList()
        inner.split("},{")
            .map { it.trim().removePrefix("{").removeSuffix("}") }
            .map { obj ->
                val f = parseFields(obj)
                BackupFileMeta(
                    name = f["name"] ?: "",
                    mimeType = f["mimeType"] ?: "",
                    category = f["category"],
                    description = f["description"],
                    encrypted = f["encrypted"] == "true",
                )
            }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseFields(obj: String): Map<String, String?> {
    val map = mutableMapOf<String, String?>()
    val regex = Regex(""""(\w+)"\s*:\s*(?:"((?:[^"\\]|\\.)*)"|null|(true|false))""")
    for (m in regex.findAll(obj)) {
        val key = m.groupValues[1]
        val strVal = m.groupValues[2].ifEmpty { null }
        val literal = m.groupValues[3].ifEmpty { null }
        map[key] = strVal ?: literal
    }
    return map
}

private fun js(s: String) = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
private fun jsn(s: String?) = if (s == null) "null" else js(s)
