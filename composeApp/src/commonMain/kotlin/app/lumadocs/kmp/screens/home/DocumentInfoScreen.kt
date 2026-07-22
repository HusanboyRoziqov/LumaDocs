package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.platform.isOnline
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
import app.lumadocs.kmp.ui.formatExpiry
import app.lumadocs.kmp.ui.formatSize
import coil3.compose.AsyncImage
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
    val isImage = file.mimeType.startsWith("image/")
    val days = expiryDaysOf(file)
    // Images to page through when the document was uploaded as a folder of pages.
    val imagePages = remember(pages) {
        pages.filter { it.mimeType.startsWith("image/") }.ifEmpty { listOf(file) }
    }
    val pagerState = rememberPagerState { imagePages.size }
    // The page the user is currently viewing — what "Open" acts on.
    val currentFile = imagePages.getOrNull(pagerState.currentPage) ?: file

    var saveInFlight by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) { if (saveInFlight && !isSaving) { saveInFlight = false; showEdit = false } }

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CircleButton(LumaIcons.Back, onBack)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("${cat.label.uppercase()} · $mimeLabel", fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.2.sp)
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
                    Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(20.dp)).background(c.bg2).clickable { onOpen(currentFile) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (imagePages.size > 1) {
                        // Swipeable pager over each page/image in the folder.
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            val pf = imagePages[page]
                            when {
                                pf.id == file.id && previewBitmap != null ->
                                    Image(previewBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                !pf.thumbnailLink.isNullOrBlank() && isOnline() ->
                                    AsyncImage(model = pf.thumbnailLink, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(fileTypeIcon(mimeLabel), null, tint = c.textMute, modifier = Modifier.size(72.dp))
                                }
                            }
                        }
                        // Page counter
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xB3000000)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("${pagerState.currentPage + 1} / ${imagePages.size}", fontFamily = LumaMono, fontSize = 11.sp, color = Color.White)
                        }
                        // Dots
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(imagePages.size) { i ->
                                Box(Modifier.size(if (i == pagerState.currentPage) 8.dp else 6.dp).clip(CircleShape).background(if (i == pagerState.currentPage) c.accent else Color(0x80FFFFFF)))
                            }
                        }
                    } else {
                        when {
                            previewBitmap != null -> Image(previewBitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            isLoadingPreview -> CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            file.thumbnailLink != null && isOnline() -> AsyncImage(model = file.thumbnailLink, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else -> Icon(fileTypeIcon(mimeLabel), null, tint = c.textMute, modifier = Modifier.size(72.dp))
                        }
                    }
                    // Open pill
                    Row(
                        Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xB3000000)).border(1.dp, c.hairline2, RoundedCornerShape(999.dp)).clickable { onOpen(currentFile) }.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(LumaIcons.Eye, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Open", fontFamily = LumaUi, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Meta card
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 6.dp).clip(RoundedCornerShape(20.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(20.dp))) {
                MetaRow("CATEGORY") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(cat.hue))
                        Text(cat.label, fontFamily = LumaUi, fontSize = 13.5.sp, color = c.text)
                    }
                }
                MetaRow("TYPE") { Text("$mimeLabel · ${formatSize(file.size)}", fontFamily = LumaUi, fontSize = 13.5.sp, color = c.text) }
                if (!file.expiryDate.isNullOrBlank()) {
                    MetaRow("EXPIRES", isLast = file.encrypted.not()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatExpiry(file.expiryDate), fontFamily = LumaUi, fontSize = 13.5.sp, color = if ((days ?: 999) <= 30) c.warn else c.text)
                            if (days != null) Text(" · in ${days}d", fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute)
                        }
                    }
                }
                MetaRow("SECURITY", isLast = true) {
                    Text(if (file.encrypted) "Encrypted · AES-256" else "Not encrypted", fontFamily = LumaUi, fontSize = 13.5.sp, color = if (file.encrypted) c.accentHi else c.textDim)
                }
            }

            // Notes card
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("NOTES", fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.4.sp)
                    Row(Modifier.clickable { showEdit = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(LumaIcons.Edit, null, tint = c.accent, modifier = Modifier.size(12.dp))
                        Text("Edit", fontFamily = LumaUi, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = c.accent)
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
                            Text("Add a note — reminders, reference numbers, renewal steps…", fontFamily = LumaUi, fontSize = 13.5.sp, color = c.textMute)
                        }
                    } else {
                        Text(note, fontFamily = LumaUi, fontSize = 13.5.sp, lineHeight = 21.sp, color = c.text)
                    }
                }
            }

            // Actions
            Row(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(LumaIcons.Share, "Share", Modifier.weight(1f), onClick = onShare)
                ActionButton(LumaIcons.Eye, "Open", Modifier.weight(1f), onClick = { onOpen(currentFile) })
                ActionButton(LumaIcons.Trash, "Delete", Modifier.weight(1f), danger = true, onClick = { showDeleteConfirm = true })
            }
        }
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
            title = { Text("Delete document?", fontFamily = LumaUi, fontWeight = FontWeight.SemiBold) },
            text = { Text("This permanently removes it from your vault and Google Drive.", fontFamily = LumaUi) },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = c.err, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = c.textDim) } },
        )
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
                Text("Cancel", color = c.textDim, fontFamily = LumaUi, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
                Text("Edit Document", color = c.text, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
                } else {
                    Text("Save", color = if (name.isBlank()) c.textMute else c.accent, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(enabled = name.isNotBlank()) { onSave(name.trim(), notes.trim().ifEmpty { null }, expiry) })
                }
            }

            SectionLabel("Name", Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            EditField { EditInline(name, "Document name", { name = it }) }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Category", Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DocCategory.entries) { cat -> CategoryChip(cat, active = category == cat, onClick = { category = cat }) }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Expiry · optional", Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            EditField {
                Row(Modifier.clickable { showDatePicker = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(LumaIcons.Calendar, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(expiry ?: "Tap to pick a date", fontFamily = LumaUi, fontSize = 15.sp, color = if (expiry != null) c.text else c.textMute, modifier = Modifier.weight(1f))
                    if (expiry != null) Text("Clear", fontFamily = LumaUi, fontSize = 13.sp, color = c.accent, modifier = Modifier.clickable { expiry = null })
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel("Notes", Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp)).padding(16.dp)) {
                if (notes.isEmpty()) Text("Add reminders, reference numbers, renewal steps…", color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp)
                BasicTextField(value = notes, onValueChange = { notes = it.take(500) }, textStyle = TextStyle(color = c.text, fontFamily = LumaUi, fontSize = 14.sp, lineHeight = 21.sp), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.06f)).border(1.dp, c.accent.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(LumaIcons.Lock, null, tint = c.accent, modifier = Modifier.size(14.dp))
                Text("Notes are encrypted at rest and synced to your Google Drive. Only you can read them.", fontFamily = LumaUi, fontSize = 11.5.sp, lineHeight = 17.sp, color = c.textDim)
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
