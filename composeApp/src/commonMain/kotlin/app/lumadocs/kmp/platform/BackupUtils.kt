package app.lumadocs.kmp.platform

data class BackupFileMeta(
    val name: String,
    val mimeType: String,
    val category: String?,
    val description: String?,
    val encrypted: Boolean,
)

expect suspend fun buildAndShareBackup(
    metas: List<BackupFileMeta>,
    fetchFileBytes: suspend (index: Int) -> ByteArray?,
)

expect suspend fun importBackupFromUri(
    uriString: String,
    onFileFound: suspend (meta: BackupFileMeta, bytes: ByteArray) -> Unit,
): Boolean
