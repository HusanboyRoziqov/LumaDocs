package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.lumadocs.kmp.IncomingFile
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.shareContent
import app.lumadocs.kmp.platform.showToast
import app.lumadocs.kmp.screens.IncomingFileViewer
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.services.GoogleDriveRepository
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.utils.PreviewCache
import app.lumadocs.kmp.utils.decodeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.file_deleted
import lumadocs.composeapp.generated.resources.no_internet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Detail sheet for a folder: reuses [DocumentDetailScreen] but swaps the single-image preview
 * for a swipeable [HorizontalPager] over the folder's files. Metadata and actions reflect the
 * page currently shown.
 */
@Composable
fun FolderDetailRoute(
    files: List<DriveFile>,
    folderName: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onDeleted: () -> Unit = {},
) {
    if (files.isEmpty()) {
        onClose()
        return
    }
    val repository = koinInject<GoogleDriveRepository>()
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(Res.string.file_deleted)
    val noInternetMsg = stringResource(Res.string.no_internet)
    val pagerState = rememberPagerState(pageCount = { files.size })
    val current = files[pagerState.currentPage.coerceIn(0, files.size - 1)]
    var showFullscreen by remember { mutableStateOf(false) }

    DocumentDetailScreen(
        file = current,
        previewBitmap = null,
        isLoadingPreview = false,
        onClose = onClose,
        onOpen = { onClose() },
        onShare = {
            scope.launch {
                val bytes = runCatching {
                    repository.getFileContent(current.id, isEncrypted = current.encrypted)
                }.getOrNull()
                if (bytes != null) {
                    shareContent(bytes, shareFileName(current.name, current.mimeType), null)
                }
            }
        },
        onDelete = {
            if (!isOnline()) {
                showToast(noInternetMsg)
            } else {
                scope.launch {
                    if (runCatching { repository.deleteFile(current.id) }.getOrDefault(false)) {
                        showToast(deletedMsg)
                        onDeleted()
                    }
                }
            }
        },
        onSave = { _, _, _ -> },
        previewOverride = {
            Box(Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    PagerPage(repository, files[page])
                }
                if (files.size > 1) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(files.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                                    .background(
                                        color = if (i == pagerState.currentPage) nBrand100 else Color.White.copy(
                                            alpha = 0.5f
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Tap to view the current page full-screen
                Surface(
                    onClick = { showFullscreen = true },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "Full screen",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }
        }
    )

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullscreenPage(repository, current) { showFullscreen = false }
        }
    }
}

@Composable
private fun FullscreenPage(
    repository: GoogleDriveRepository,
    file: DriveFile,
    onClose: () -> Unit,
) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val bytes by produceState<ByteArray?>(null, file.id) {
        value = PreviewCache.get(dataStore, file.id) ?: runCatching {
            repository.getFileContent(file.id, isEncrypted = file.encrypted)
        }.getOrNull()?.also { PreviewCache.put(dataStore, file.id, it) }
    }
    val loaded by produceState(false, file.id, bytes) { value = bytes != null }

    val content = bytes
    if (content != null) {
        IncomingFileViewer(
            file = IncomingFile(bytes = content, fileName = file.name, mimeType = file.mimeType),
            onClose = onClose
        )
    } else {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (!loaded) CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun PagerPage(repository: GoogleDriveRepository, file: DriveFile) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val bytes by produceState<ByteArray?>(null, file.id) {
        value = PreviewCache.get(dataStore, file.id) ?: runCatching {
            repository.getFileContent(file.id, isEncrypted = file.encrypted)
        }.getOrNull()?.also { PreviewCache.put(dataStore, file.id, it) }
    }
    val loaded by produceState(false, file.id, bytes) {
        // becomes true once the fetch has completed (even if it returned null)
        value = bytes != null
    }
    val bitmap by produceState<ImageBitmap?>(null, bytes) {
        value = bytes?.let { withContext(Dispatchers.Default) { decodeImageBitmap(it) } }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            !loaded -> CircularProgressIndicator(color = nBrand100)

            else -> Icon(
                imageVector = fileTypeIcon(getMimeTypeLabel(file.mimeType)),
                contentDescription = null,
                tint = nBlack300,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
