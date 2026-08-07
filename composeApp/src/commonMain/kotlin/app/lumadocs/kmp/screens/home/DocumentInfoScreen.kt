package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.platform.rememberNotificationPermissionLauncher
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.CategoryChip
import app.lumadocs.kmp.ui.DocCategory
import app.lumadocs.kmp.ui.ExpiryDatePickerDialog
import app.lumadocs.kmp.ui.MetaRow
import app.lumadocs.kmp.ui.SectionLabel
import app.lumadocs.kmp.ui.categoryOf
import app.lumadocs.kmp.ui.expiryDaysOf
import app.lumadocs.kmp.ui.formatExpiryLocalized
import app.lumadocs.kmp.ui.localizedLabel
import app.lumadocs.kmp.ui.localizedMimeLabel
import app.lumadocs.kmp.ui.formatSizeLocalized
import app.lumadocs.kmp.ui.sizedThumb
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.action_clear
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.close
import lumadocs.composeapp.generated.resources.delete
import lumadocs.composeapp.generated.resources.delete_document_message
import lumadocs.composeapp.generated.resources.delete_document_title
import lumadocs.composeapp.generated.resources.detail_expires_in
import lumadocs.composeapp.generated.resources.document_category
import lumadocs.composeapp.generated.resources.document_name_hint
import lumadocs.composeapp.generated.resources.edit
import lumadocs.composeapp.generated.resources.edit_document
import lumadocs.composeapp.generated.resources.encrypted_aes
import lumadocs.composeapp.generated.resources.expiry_optional
import lumadocs.composeapp.generated.resources.field_name
import lumadocs.composeapp.generated.resources.meta_category
import lumadocs.composeapp.generated.resources.meta_expires
import lumadocs.composeapp.generated.resources.meta_notes
import lumadocs.composeapp.generated.resources.meta_security
import lumadocs.composeapp.generated.resources.meta_type
import lumadocs.composeapp.generated.resources.not_encrypted
import lumadocs.composeapp.generated.resources.notes_edit_placeholder
import lumadocs.composeapp.generated.resources.notes_encrypted_note
import lumadocs.composeapp.generated.resources.notes_label
import lumadocs.composeapp.generated.resources.notes_placeholder
import lumadocs.composeapp.generated.resources.page_indicator
import lumadocs.composeapp.generated.resources.save
import lumadocs.composeapp.generated.resources.share
import lumadocs.composeapp.generated.resources.tap_to_pick_date
import org.jetbrains.compose.resources.stringResource
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import okio.Path.Companion.toPath
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Candlelight full-screen detail page for a single document. Keeps the original signature so
 * [DocumentDetailRoute] wiring (preview, share, delete, save) is unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentInfoScreen(
    file: DriveFile,
    previewBitmap: ImageBitmap?,
    pages: List<DriveFile> = listOf(file),
    localPaths: Map<String, String> = emptyMap(),
    isLoadingPreview: Boolean = false,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onOpen: (DriveFile) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSave: (newTitle: String, newDescription: String?, newExpiryDate: String?) -> Unit,
) {
    val c = LocalLumaColors.current
    val cat = categoryOf(file)
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val mimeLabel = remember(file.mimeType) { getMimeTypeLabel(file.mimeType) }
    val mimeLabelText = localizedMimeLabel(file.mimeType)
    val days = expiryDaysOf(file)
    // Images to page through when the document was uploaded as a folder of pages.
    val imagePages = remember(pages) {
        pages.filter { it.mimeType.startsWith("image/") }.ifEmpty { listOf(file) }
    }
    val pagerState = rememberPagerState { imagePages.size }
    // The page the user is currently viewing — what share/open act on.
    val currentFile = imagePages.getOrNull(pagerState.currentPage) ?: file
    // Images open in the app's own viewer; anything else (PDF, docs) still hands off externally.
    var viewerPage by remember { mutableStateOf<Int?>(null) }
    val openCurrent = {
        if (currentFile.mimeType.startsWith("image/")) viewerPage = pagerState.currentPage
        else onOpen(currentFile)
    }

    var saveInFlight by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) { if (saveInFlight && !isSaving) { saveInFlight = false; showEdit = false } }

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CircleButton(LumaIcons.Back, onBack)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("${cat.localizedLabel().uppercase()} · $mimeLabelText", fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.2.sp)
                Text(file.name, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            CircleButton(LumaIcons.Edit, { showEdit = true }, iconSize = 16.dp)
        }

        Column(Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero preview
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(20.dp)).background(c.bg2).clickable { openCurrent() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (imagePages.size > 1) {
                        // Swipeable pager over each page/image in the folder.
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val pf = imagePages[page]
                            PageImage(
                                page = pf,
                                localPath = localPaths[pf.id],
                                fallbackBitmap = previewBitmap.takeIf { pf.id == file.id },
                                mimeLabel = mimeLabel,
                            )
                        }
                        // Page counter
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xB3000000)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(stringResource(Res.string.page_indicator, pagerState.currentPage + 1, imagePages.size), fontFamily = LumaMono, fontSize = 11.sp, color = Color.White)
                        }
                        // Dots
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(imagePages.size) { i ->
                                Box(Modifier.size(if (i == pagerState.currentPage) 8.dp else 6.dp).clip(CircleShape).background(if (i == pagerState.currentPage) c.accent else Color(0x80FFFFFF)))
                            }
                        }
                    } else {
                        PageImage(
                            page = file,
                            localPath = localPaths[file.id],
                            fallbackBitmap = previewBitmap,
                            mimeLabel = mimeLabel,
                            isLoading = isLoadingPreview,
                        )
                    }
                }
            }

            // Meta card
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp).clip(RoundedCornerShape(20.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(20.dp))) {
                MetaRow(stringResource(Res.string.meta_category)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(cat.hue))
                        Text(cat.localizedLabel(), fontFamily = LumaUi, fontSize = 13.5.sp, color = c.text)
                    }
                }
                MetaRow(stringResource(Res.string.meta_type)) { Text("$mimeLabelText · ${formatSizeLocalized(file.size)}", fontFamily = LumaUi, fontSize = 13.5.sp, color = c.text) }
                if (!file.expiryDate.isNullOrBlank()) {
                    MetaRow(stringResource(Res.string.meta_expires), isLast = file.encrypted.not()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatExpiryLocalized(file.expiryDate), fontFamily = LumaUi, fontSize = 13.5.sp, color = if ((days ?: 999) <= 30) c.warn else c.text)
                            if (days != null) Text(stringResource(Res.string.detail_expires_in, days), fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute)
                        }
                    }
                }
                MetaRow(stringResource(Res.string.meta_security), isLast = true) {
                    Text(stringResource(if (file.encrypted) Res.string.encrypted_aes else Res.string.not_encrypted), fontFamily = LumaUi, fontSize = 13.5.sp, color = if (file.encrypted) c.accentHi else c.textDim)
                }
            }

            // Notes card
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.meta_notes), fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.4.sp)
                    Row(Modifier.clickable { showEdit = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(LumaIcons.Edit, null, tint = c.accent, modifier = Modifier.size(12.dp))
                        Text(stringResource(Res.string.edit), fontFamily = LumaUi, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.accent)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val note = file.description
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accent.copy(alpha = 0.03f)).border(1.dp, if (note.isNullOrBlank()) c.accent.copy(alpha = 0.2f) else c.hairline, RoundedCornerShape(16.dp)).clickable { showEdit = true }.padding(16.dp),
                ) {
                    if (note.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(LumaIcons.Edit, null, tint = c.accent, modifier = Modifier.size(14.dp))
                            Text(stringResource(Res.string.notes_placeholder), fontFamily = LumaUi, fontSize = 13.5.sp, color = c.textMute)
                        }
                    } else {
                        Text(note, fontFamily = LumaUi, fontSize = 13.5.sp, lineHeight = 21.sp, color = c.text)
                    }
                }
            }

            // Actions
            // Tapping the preview is how you view a document now — no "Open" action needed here.
            Row(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(LumaIcons.Share, stringResource(Res.string.share), Modifier.weight(1f), onClick = onShare)
                ActionButton(LumaIcons.Trash, stringResource(Res.string.delete), Modifier.weight(1f), danger = true, onClick = { showDeleteConfirm = true })
            }
        }
    }

    viewerPage?.let { start ->
        FullscreenPageViewer(
            pages = imagePages,
            localPaths = localPaths,
            startPage = start,
            fallbackBitmap = previewBitmap,
            primaryId = file.id,
            onDismiss = { viewerPage = null },
        )
    }

    if (showEdit) {
        EditDocumentSheet(
            file = file, isSaving = isSaving,
            onSave = { t, d, e -> saveInFlight = true; onSave(t, d, e) },
            onDismiss = { showEdit = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = c.bg2, titleContentColor = c.text, textContentColor = c.textDim,
            title = { Text(stringResource(Res.string.delete_document_title), fontFamily = LumaUi, fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(Res.string.delete_document_message), fontFamily = LumaUi) },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text(stringResource(Res.string.delete), color = c.err, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(Res.string.cancel), color = c.textDim) } },
        )
    }
}

/**
 * One page of the hero preview, layered worst-to-best so the box is never empty:
 *
 * 1. a large (=s1600) Drive thumbnail as the instant backdrop — Coil serves it from its disk cache
 *    on later visits, and it's keyed by file id so a rotated/expired Drive link still hits cache;
 * 2. the full-quality bytes from [PreviewCache], decoded straight off disk by Coil (downsampled to
 *    the box, so a 3 MB photo costs a fraction of a full-size decode);
 * 3. an already-decoded bitmap when the ViewModel happens to hold one.
 *
 * Nothing here is gated on connectivity: everything cached renders offline, and the file-type icon
 * only shows when there is genuinely no image to draw.
 */
@Composable
private fun PageImage(
    page: DriveFile,
    localPath: String?,
    fallbackBitmap: ImageBitmap?,
    mimeLabel: String,
    isLoading: Boolean = false,
) {
    val c = LocalLumaColors.current
    val ctx = LocalPlatformContext.current

    val thumbRequest = remember(page.id, page.thumbnailLink) {
        if (page.thumbnailLink.isNullOrBlank()) null
        else ImageRequest.Builder(ctx)
            .data(sizedThumb(page.thumbnailLink, 1600))
            .memoryCacheKey("hero_${page.id}")
            .diskCacheKey("hero_${page.id}")
            .placeholderMemoryCacheKey("hero_${page.id}")
            .build()
    }
    val fullRequest = remember(page.id, localPath) {
        localPath?.let {
            ImageRequest.Builder(ctx)
                .data(it.toPath())
                .memoryCacheKey("full_${page.id}")
                .placeholderMemoryCacheKey("full_${page.id}")
                .build()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (thumbRequest != null) {
            AsyncImage(thumbRequest, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        when {
            fullRequest != null ->
                AsyncImage(fullRequest, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            fallbackBitmap != null ->
                Image(fallbackBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            thumbRequest == null && isLoading ->
                CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            thumbRequest == null ->
                Icon(fileTypeIcon(mimeLabel), null, tint = c.textMute, modifier = Modifier.size(72.dp))
        }
    }
}

/**
 * In-app full-screen viewer: pinch/drag to zoom, swipe between the document's pages, tap or back to
 * close. Reads the same cached files as the detail hero, so opening a photo never leaves the app
 * (and never needs the gallery) — the external handoff is reserved for PDFs and other documents.
 */
@Composable
private fun FullscreenPageViewer(
    pages: List<DriveFile>,
    localPaths: Map<String, String>,
    startPage: Int,
    fallbackBitmap: ImageBitmap?,
    primaryId: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val ctx = LocalPlatformContext.current
        val pagerState = rememberPagerState(initialPage = startPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) { pages.size }
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        // A new page starts unzoomed, so a swipe never lands mid-pan on the next photo.
        LaunchedEffect(pagerState.currentPage) { scale = 1f; offsetX = 0f; offsetY = 0f }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f; offsetY = 0f
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            // Zoomed in, horizontal drags pan the photo instead of flipping the page.
            HorizontalPager(state = pagerState, userScrollEnabled = scale <= 1f, modifier = Modifier.fillMaxSize()) { page ->
                val pf = pages[page]
                val zoomed = page == pagerState.currentPage
                val model = remember(pf.id, localPaths[pf.id], pf.thumbnailLink) {
                    localPaths[pf.id]?.let { path ->
                        ImageRequest.Builder(ctx).data(path.toPath())
                            .memoryCacheKey("full_${pf.id}").placeholderMemoryCacheKey("hero_${pf.id}").build()
                    } ?: pf.thumbnailLink?.let { link ->
                        ImageRequest.Builder(ctx).data(sizedThumb(link, 1600))
                            .memoryCacheKey("hero_${pf.id}").diskCacheKey("hero_${pf.id}")
                            .placeholderMemoryCacheKey("hero_${pf.id}").build()
                    }
                }
                val imageModifier = Modifier.fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (zoomed) scale else 1f,
                        scaleY = if (zoomed) scale else 1f,
                        translationX = if (zoomed) offsetX else 0f,
                        translationY = if (zoomed) offsetY else 0f,
                    )
                    .transformable(transformState, enabled = zoomed)

                Box(
                    Modifier.fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = scale <= 1f,
                    ) { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        model != null -> AsyncImage(model, null, imageModifier, contentScale = ContentScale.Fit)
                        pf.id == primaryId && fallbackBitmap != null ->
                            Image(fallbackBitmap, null, imageModifier, contentScale = ContentScale.Fit)
                        else -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
            }

            if (pages.size > 1) {
                Box(
                    Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(16.dp)
                        .clip(RoundedCornerShape(999.dp)).background(Color(0xB3000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(Res.string.page_indicator, pagerState.currentPage + 1, pages.size), fontFamily = LumaMono, fontSize = 12.sp, color = Color.White)
                }
            }

            Box(
                Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(16.dp)
                    .size(44.dp).clip(CircleShape).background(Color(0x26FFFFFF)).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LumaIcons.Close, stringResource(Res.string.close), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, onClick: () -> Unit, iconSize: androidx.compose.ui.unit.Dp = 18.dp) {
    val c = LocalLumaColors.current
    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x0FFFFFFF)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = c.text, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val c = LocalLumaColors.current
    Column(
        modifier.height(58.dp).clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (danger) c.err else c.textDim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontFamily = LumaUi, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = if (danger) c.err else c.text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDocumentSheet(
    file: DriveFile,
    isSaving: Boolean,
    onSave: (String, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLumaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(file.name) }
    var notes by remember { mutableStateOf(file.description ?: "") }
    var expiry by remember { mutableStateOf(file.expiryDate?.ifEmpty { null }) }
    var category by remember { mutableStateOf(categoryOf(file)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val requestNotif = rememberNotificationPermissionLauncher { }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = c.bg, dragHandle = { BottomSheetDefaults.DragHandle(color = c.bg2) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.cancel), color = c.textDim, fontFamily = LumaUi, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
                Text(stringResource(Res.string.edit_document), color = c.text, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.save), color = if (name.isBlank()) c.textMute else c.accent, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(enabled = name.isNotBlank()) { onSave(name.trim(), notes.trim().ifEmpty { null }, expiry) })
                }
            }

            SectionLabel(stringResource(Res.string.field_name), Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            EditField { EditInline(name, stringResource(Res.string.document_name_hint), { name = it }) }
            Spacer(Modifier.height(20.dp))

            SectionLabel(stringResource(Res.string.document_category), Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DocCategory.entries) { cat -> CategoryChip(cat, active = category == cat, onClick = { category = cat }) }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel(stringResource(Res.string.expiry_optional), Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            EditField {
                Row(Modifier.clickable { showDatePicker = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(LumaIcons.Calendar, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(expiry ?: stringResource(Res.string.tap_to_pick_date), fontFamily = LumaUi, fontSize = 15.sp, color = if (expiry != null) c.text else c.textMute, modifier = Modifier.weight(1f))
                    if (expiry != null) Text(stringResource(Res.string.action_clear), fontFamily = LumaUi, fontSize = 13.sp, color = c.accent, modifier = Modifier.clickable { expiry = null })
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel(stringResource(Res.string.notes_label), Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(16.dp)) {
                if (notes.isEmpty()) Text(stringResource(Res.string.notes_edit_placeholder), color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp)
                BasicTextField(value = notes, onValueChange = { notes = it.take(500) }, textStyle = TextStyle(color = c.text, fontFamily = LumaUi, fontSize = 14.sp, lineHeight = 21.sp), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.06f)).border(1.dp, c.accent.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(LumaIcons.Lock, null, tint = c.accent, modifier = Modifier.size(14.dp))
                Text(stringResource(Res.string.notes_encrypted_note), fontFamily = LumaUi, fontSize = 11.5.sp, lineHeight = 17.sp, color = c.textDim)
            }
        }
    }

    if (showDatePicker) {
        ExpiryDatePickerDialog(
            currentIso = expiry,
            onPicked = { expiry = it; requestNotif(); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun EditField(content: @Composable () -> Unit) {
    val c = LocalLumaColors.current
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
}

@Composable
private fun EditInline(value: String, placeholder: String, onChange: (String) -> Unit) {
    val c = LocalLumaColors.current
    Box(contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(placeholder, color = c.textMute, fontFamily = LumaUi, fontSize = 16.sp)
        BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(color = c.text, fontFamily = LumaUi, fontSize = 16.sp, fontWeight = FontWeight.Medium), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth())
    }
}

// ExpiryDatePickerDialog now lives in app.lumadocs.kmp.ui and is shared with the scan save form.
