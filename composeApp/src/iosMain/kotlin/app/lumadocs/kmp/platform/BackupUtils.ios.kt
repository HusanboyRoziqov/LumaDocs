package app.lumadocs.kmp.platform

actual suspend fun buildAndShareBackup(
    metas: List<BackupFileMeta>,
    fetchFileBytes: suspend (index: Int) -> ByteArray?,
) {  }

actual suspend fun importBackupFromUri(
    uriString: String,
    onFileFound: suspend (meta: BackupFileMeta, bytes: ByteArray) -> Unit,
): Boolean = false
