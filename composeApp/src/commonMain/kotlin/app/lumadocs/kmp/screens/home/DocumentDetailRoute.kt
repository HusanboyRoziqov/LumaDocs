package app.lumadocs.kmp.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.openFileExternally
import app.lumadocs.kmp.platform.shareContent
import app.lumadocs.kmp.platform.showToast
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.utils.decodeImageBitmap
import app.lumadocs.kmp.viewmodels.DocumentDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.changes_saved
import lumadocs.composeapp.generated.resources.error_save_failed
import lumadocs.composeapp.generated.resources.file_deleted
import lumadocs.composeapp.generated.resources.no_internet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DocumentDetailRoute(
    file: DriveFile,
    pages: List<DriveFile> = listOf(file),
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onDeleted: () -> Unit = {},
    onSaved: (updated: DriveFile) -> Unit = {},
) {
    val viewModel = koinViewModel<DocumentDetailViewModel>()
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(Res.string.file_deleted)
    val noInternetMsg = stringResource(Res.string.no_internet)
    val savedMsg = stringResource(Res.string.changes_saved)
    val saveFailedMsg = stringResource(Res.string.error_save_failed)

    LaunchedEffect(file.id) {
        viewModel.setFile(file)
        viewModel.loadPreview()
    }

    // Pull every page's full-quality bytes into the disk cache so the pager renders real photos
    // (Drive thumbnails are ~220px and expire) and keeps showing them offline.
    LaunchedEffect(pages) { viewModel.ensureCached(pages) }

    val currentFile by viewModel.file.collectAsState()
    val displayFile = currentFile ?: file

    val localPaths by viewModel.localPaths.collectAsState()
    val previewBytes by viewModel.previewBytes.collectAsState()
    val isLoadingPreview by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    // Only decode in-process when the page isn't already on disk: Coil decodes the cached file
    // downsampled to the box, while decodeImageBitmap builds a full-size bitmap (tens of MB for a
    // phone photo). The bitmap stays as the fallback for the brief window before the cache lands.
    val hasCachedFile = localPaths.containsKey(displayFile.id)
    val previewBitmap by produceState<ImageBitmap?>(null, previewBytes, hasCachedFile) {
        value = if (hasCachedFile) null else previewBytes?.let { bytes ->
            withContext(Dispatchers.Default) { decodeImageBitmap(bytes) }
        }
    }

    DocumentInfoScreen(
        file = displayFile,
        previewBitmap = previewBitmap,
        pages = pages,
        localPaths = localPaths,
        isLoadingPreview = isLoadingPreview,
        isSaving = isSaving,
        onBack = onClose,
        onOpen = { target ->
            scope.launch {
                // Open the page the user is currently viewing (reuse the loaded bytes for the
                // primary file; download the specific page otherwise).
                val bytes = if (target.id == displayFile.id) {
                    viewModel.previewBytes.value ?: viewModel.contentFor(target)
                } else {
                    viewModel.contentFor(target)
                }
                if (bytes != null) {
                    openFileExternally(
                        bytes,
                        shareFileName(target.name, target.mimeType),
                        target.mimeType
                    )
                } else {
                    showToast(noInternetMsg)
                }
            }
        },
        onShare = {
            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                // previewBytes are already decrypted — share the plain photo so external
                // apps open it as an image and never need to open LumaDocs.
                val bytes = viewModel.previewBytes.value
                if (bytes != null) {
                    shareContent(bytes, shareFileName(displayFile.name, displayFile.mimeType), null)
                } else {
                    val link = viewModel.getShareLink()
                    shareContent(null, null, link)
                }
            }
        },
        onDelete = {
            if (!isOnline()) {
                showToast(noInternetMsg)
            } else {
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                    val deleted = viewModel.deleteCurrentFile()
                    if (deleted) {
                        showToast(deletedMsg)
                        onDeleted()
                    }
                }
            }
        },
        onSave = { newTitle, newDesc, newExpiry ->
            if (!isOnline()) {
                showToast(noInternetMsg)
            } else {
                scope.launch {
                    val success = viewModel.updateMetadata(newTitle, newDesc, newExpiry)
                    if (success) {
                        showToast(savedMsg)
                        val updated = viewModel.file.value
                        if (updated != null) onSaved(updated)
                    } else {
                        showToast(saveFailedMsg)
                    }
                }
            }
        }
    )
}

/** Ensures a shared file has a sensible extension so external apps handle it by type. */
internal fun shareFileName(name: String, mimeType: String): String {
    val lower = name.lowercase()
    val known = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".pdf")
    if (known.any { lower.endsWith(it) }) return name
    val ext = when {
        mimeType.contains("png") -> ".png"
        mimeType.contains("webp") -> ".webp"
        mimeType.contains("gif") -> ".gif"
        mimeType.contains("pdf") -> ".pdf"
        mimeType.startsWith("image/") -> ".jpg"
        else -> ""
    }
    return name + ext
}
