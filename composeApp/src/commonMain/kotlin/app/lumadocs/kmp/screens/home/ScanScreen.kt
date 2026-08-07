package app.lumadocs.kmp.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumadocs.kmp.VaultEvents
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.navigation.AppBackHandler
import app.lumadocs.kmp.platform.InAppCameraPreview
import app.lumadocs.kmp.platform.PickedFile
import app.lumadocs.kmp.platform.inAppCameraSupported
import app.lumadocs.kmp.platform.rememberCameraCapture
import app.lumadocs.kmp.platform.rememberFilePicker
import app.lumadocs.kmp.platform.rememberNotificationPermissionLauncher
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.CategoryChip
import app.lumadocs.kmp.ui.DocCategory
import app.lumadocs.kmp.ui.ExpiryDatePickerDialog
import app.lumadocs.kmp.ui.PillButton
import app.lumadocs.kmp.ui.SectionLabel
import app.lumadocs.kmp.ui.ThumbSize
import app.lumadocs.kmp.utils.decodeImageBitmap
import app.lumadocs.kmp.viewmodels.AddScreenViewModel
import app.lumadocs.kmp.viewmodels.SelectedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ScanScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    vm: AddScreenViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val files = state.selectedFiles
    var stage by remember { mutableStateOf("camera") }
    var category by remember { mutableStateOf(DocCategory.IDENTITY) }

    val embedded = inAppCameraSupported()
    var captureTrigger by remember { mutableStateOf(0) }
    var flashOn by remember { mutableStateOf(false) }
    // AUTO: one shot → straight to crop. MANUAL: stack multiple pages, Done → crop.
    var autoMode by remember { mutableStateOf(true) }

    val onPhotoCaptured: (PickedFile?) -> Unit = { picked ->
        if (picked != null) {
            vm.addFileToList(picked.fileName, picked.mimeType, picked.bytes)
            if (autoMode) stage = "edit"
        }
    }

    val launchCamera =
        rememberCameraCapture(onPhotoCaptured) // external system camera (iOS fallback)
    val launchGallery = rememberFilePicker(arrayOf("image/*")) { picked ->
        picked.forEach { vm.addFileToList(it.fileName, it.mimeType, it.bytes) }
        // Picked from the gallery → go straight to the crop/review screen.
        if (picked.isNotEmpty()) stage = "edit"
    }
    val launchPdf = rememberFilePicker(arrayOf("application/pdf")) { picked ->
        picked.forEach { vm.addFileToList(it.fileName, it.mimeType, it.bytes) }
        if (picked.isNotEmpty()) stage = "save"
    }

    // Fresh scan session on entry. With an embedded preview we show it live; otherwise fall
    // back to opening the system camera immediately.
    LaunchedEffect(Unit) {
        vm.clearSelectedFiles()
        if (!embedded) launchCamera()
    }

    // Back walks the scan flow one stage back instead of dropping the whole session; on the
    // camera stage it falls through to the nav back stack (which leaves Scan).
    // While an upload is in flight the handler stays enabled but does nothing: leaving mid-upload
    // would strand half a document in Drive.
    AppBackHandler(enabled = stage == "save") {
        if (!state.isLoading) {
            stage = if (files.any { it.mimeType.startsWith("image/") }) "edit" else "camera"
        }
    }

    // Set the moment an upload succeeds, so the "no pages left" rule below doesn't bounce the
    // user through the camera on the way out.
    var leaving by remember { mutableStateOf(false) }

    // Removing the last page leaves nothing to save — drop back to the camera instead of
    // stranding the user on an empty form.
    LaunchedEffect(stage, files.isEmpty(), state.isLoading, leaving) {
        if (stage == "save" && files.isEmpty() && !state.isLoading && !leaving) stage = "camera"
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            leaving = true
            VaultEvents.requestRefresh()
            vm.resetState()
            onSaved()
        }
    }

    when (stage) {
        // Review each photo in a swipeable filmstrip and crop them before filling in details.
        "edit" -> MediaEditorScreen(
            initial = files.filter { it.mimeType.startsWith("image/") }
                .map { PickedFile(it.fileName, it.mimeType, it.fileBytes) },
            onSend = { edited ->
                val pdfs = files.filter { !it.mimeType.startsWith("image/") }
                vm.clearSelectedFiles()
                edited.forEach { vm.addFileToList(it.fileName, it.mimeType, it.bytes) }
                pdfs.forEach { vm.addFileToList(it.fileName, it.mimeType, it.fileBytes) }
                stage = "save"
            },
            onCancel = { stage = "camera" },
        )

        "save" -> ScanSaveStage(
            files = files,
            category = category,
            onCategory = { category = it },
            isSaving = state.isLoading,
            uploadedCount = state.uploadedCount,
            totalCount = state.totalCount,
            needsDrive = state.needsGoogleDriveConnection,
            onDismissDrive = { vm.dismissConnectPrompt() },
            onRetake = { stage = "camera" },
            onAddFromGallery = launchGallery,
            // "Take a photo" walks back to the capture stage, exactly like Retake.
            onAddFromCamera = { stage = "camera" },
            onReview = { if (files.any { it.mimeType.startsWith("image/") }) stage = "edit" },
            onRemove = { vm.removeFileFromList(it) },
            onSave = { name, expiry ->
                vm.uploadMultipleFiles(
                    title = name,
                    folderName = if (files.size > 1) name else "",
                    expiryDate = expiry.ifBlank { null },
                )
            },
        )

        else -> ScanCameraStage(
            files = files,
            embedded = embedded,
            captureTrigger = captureTrigger,
            flashOn = flashOn,
            onToggleFlash = { flashOn = !flashOn },
            autoMode = autoMode,
            onMode = { autoMode = it },
            onCaptured = onPhotoCaptured,
            onBack = onBack,
            onShutter = { if (embedded) captureTrigger++ else launchCamera() },
            onGallery = launchGallery,
            onPdf = launchPdf,
            onDone = {
                if (files.any { it.mimeType.startsWith("image/") }) stage = "edit"
                else if (files.isNotEmpty()) stage = "save"
            },
        )
    }
}

@Composable
private fun ScanCameraStage(
    files: List<SelectedFile>,
    embedded: Boolean,
    captureTrigger: Int,
    flashOn: Boolean,
    onToggleFlash: () -> Unit,
    autoMode: Boolean,
    onMode: (Boolean) -> Unit,
    onCaptured: (PickedFile?) -> Unit,
    onBack: () -> Unit,
    onShutter: () -> Unit,
    onGallery: () -> Unit,
    onPdf: () -> Unit,
    onDone: () -> Unit,
) {
    val c = LocalLumaColors.current
    val pages = files.size
    val lastImage = files.lastOrNull { it.mimeType.startsWith("image/") }
    val lastBitmap by produceState<ImageBitmap?>(null, lastImage) {
        value =
            lastImage?.let { withContext(Dispatchers.Default) { decodeImageBitmap(it.fileBytes) } }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        1f,
        0.3f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "dot"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF000000))) {

        Box(
            Modifier.fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF2A2620), Color(0xFF0A0806))))
        )

        Box(Modifier.fillMaxSize().padding(top = 110.dp, bottom = 210.dp)) {
            if (embedded) {
                InAppCameraPreview(
                    modifier = Modifier.matchParentSize(),
                    captureTrigger = captureTrigger,
                    flashOn = flashOn,
                    onCaptured = onCaptured,
                    onPermissionDenied = {},
                )
            } else {
                lastBitmap?.let {
                    Image(
                        it,
                        null,
                        Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleGlassButton(LumaIcons.Close, onBack)
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0x80000000))
                        .border(1.dp, c.hairline2, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent).alpha(dotAlpha))
                    Text(
                        if (embedded) "LIVE" else if (lastBitmap != null) "IMAGE READY" else "EDGE DETECTED",
                        fontFamily = LumaMono,
                        fontSize = 11.sp,
                        color = c.accent,
                        letterSpacing = 0.6.sp
                    )
                }

                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(if (flashOn) c.accent else Color(0x80000000))
                        .border(1.dp, if (flashOn) c.accent else c.hairline2, CircleShape)
                        .clickable(onClick = onToggleFlash),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        LumaIcons.Flash, "Flash",
                        tint = if (flashOn) Color(0xFF1A1408) else Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (pages > 0) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(999.dp))
                        .background(Color(0x80000000)).padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        "$pages page${if (pages > 1) "s" else ""} added",
                        fontFamily = LumaMono,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Mode toggle. AUTO: one shot → crop. MANUAL: stack pages, Done → crop.
            // PDF opens the phone's document picker.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AUTO",
                    fontFamily = LumaMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (autoMode) c.accent else Color(0x80FFFFFF),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).clickable { onMode(true) }
                )
                Text(
                    "MANUAL",
                    fontFamily = LumaMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!autoMode) c.accent else Color(0x80FFFFFF),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).clickable { onMode(false) }
                )
                Text(
                    "PDF",
                    fontFamily = LumaMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0x80FFFFFF),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).clickable(onClick = onPdf)
                )
            }
            if (!autoMode) {
                Text(
                    "Multi-page: shoot each page, then Done",
                    fontFamily = LumaUi, fontSize = 11.sp, color = Color(0x99FFFFFF),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            // Gallery · Shutter · Done
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pick from gallery
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, c.hairline2, RoundedCornerShape(16.dp))
                        .clickable(onClick = onGallery),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        LumaIcons.Image,
                        "Pick from gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Shutter (camera)
                Box(
                    Modifier.size(78.dp).clip(CircleShape)
                        .border(3.dp, Color(0xE6FFFFFF), CircleShape).padding(4.dp)
                        .clickable(onClick = onShutter),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White)) }

                // Done
                Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Done",
                        fontFamily = LumaUi,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pages > 0) c.accent else Color(0x4DFFFFFF),
                        modifier = Modifier.clickable(onClick = onDone)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScanSaveStage(
    files: List<SelectedFile>,
    category: DocCategory,
    onCategory: (DocCategory) -> Unit,
    isSaving: Boolean,
    uploadedCount: Int,
    totalCount: Int,
    needsDrive: Boolean,
    onDismissDrive: () -> Unit,
    onRetake: () -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromCamera: () -> Unit,
    onReview: () -> Unit,
    onRemove: (index: Int) -> Unit,
    onSave: (name: String, expiry: String) -> Unit,
) {
    val c = LocalLumaColors.current
    var name by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    val requestNotif = rememberNotificationPermissionLauncher { }

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.clickable(onClick = onRetake),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(LumaIcons.Back, null, tint = c.textDim, modifier = Modifier.size(18.dp))
                Text("Retake", color = c.textDim, fontFamily = LumaUi, fontSize = 15.sp)
            }
            Text(
                "New Document",
                color = c.text,
                fontFamily = LumaUi,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(56.dp))
        }

        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            // Real captured/picked previews — tap to review & crop.
            // Index keys + raw-bytes params: SelectedFile.equals compares whole byte arrays, which
            // froze this screen (every keystroke/recomposition did multi-MB memcmp per item).
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(count = files.size, key = { it }) { i ->
                    val f = files[i]
                    Box {
                        Box(Modifier.clickable(onClick = onReview)) {
                            CapturedThumb(mimeType = f.mimeType, bytes = f.fileBytes)
                        }
                        // Drop a page you don't want before saving.
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp)
                                .size(26.dp).clip(CircleShape).background(Color(0xCC000000))
                                .clickable { onRemove(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(LumaIcons.Close, "Remove page", tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                // Adding a page lives with the pages themselves, so the bottom row is just "Save".
                item(key = "add") { AddPageCard(onClick = { showAddSheet = true }) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a page to review & crop · + to add another",
                fontFamily = LumaUi,
                fontSize = 12.sp,
                color = c.textMute
            )
            Spacer(Modifier.height(20.dp))

            if (needsDrive) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(c.err.copy(alpha = 0.1f))
                        .border(1.dp, c.err.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismissDrive).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(LumaIcons.Cloud, null, tint = c.err, modifier = Modifier.size(16.dp))
                    Text(
                        "Connect Google Drive in Settings to save. Tap to dismiss.",
                        color = c.textDim,
                        fontFamily = LumaUi,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("Name", Modifier.padding(horizontal = 0.dp)); Spacer(Modifier.height(8.dp))
            FieldBox { InlineField(name, "Document name", { name = it }) }
            Spacer(Modifier.height(20.dp))

            SectionLabel(
                "Category",
                Modifier.padding(horizontal = 0.dp)
            ); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DocCategory.entries) { cat ->
                    CategoryChip(
                        cat,
                        active = category == cat,
                        onClick = { onCategory(cat) })
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionLabel(
                "Expiry · optional",
                Modifier.padding(horizontal = 0.dp)
            ); Spacer(Modifier.height(8.dp))
            FieldBox {
                Row(
                    Modifier.clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(LumaIcons.Calendar, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(
                        expiry.ifBlank { "Tap to pick a date" },
                        fontFamily = LumaUi,
                        fontSize = 15.sp,
                        color = if (expiry.isBlank()) c.textMute else c.text,
                        modifier = Modifier.weight(1f)
                    )
                    if (expiry.isNotBlank()) Text(
                        "Clear",
                        fontFamily = LumaUi,
                        fontSize = 13.sp,
                        color = c.accent,
                        modifier = Modifier.clickable { expiry = "" })
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            if (isSaving) {
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(999.dp))
                        .background(c.accent), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = Color(0xFF1A1408),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                PillButton(
                    "Save to Vault",
                    onClick = { onSave(name.ifBlank { "Scan" }, expiry) },
                    leadingIcon = LumaIcons.Check,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Covers the whole screen while uploading: no editing, no back, no leaving half a document
    // behind. It disappears on its own when the upload finishes and the screen navigates away.
    if (isSaving) {
        UploadBlockingDialog(uploadedCount = uploadedCount, totalCount = totalCount)
    }

    if (showAddSheet) {
        AddImageSheet(
            onGallery = { showAddSheet = false; onAddFromGallery() },
            onCamera = { showAddSheet = false; onAddFromCamera() },
            onDismiss = { showAddSheet = false },
        )
    }

    if (showDatePicker) {
        ExpiryDatePickerDialog(
            currentIso = expiry.ifBlank { null },
            onPicked = { expiry = it; requestNotif(); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Modal, non-dismissible upload progress. Back press and outside taps are disabled, so the document
 * can't be abandoned halfway through; the caller navigates away as soon as the upload succeeds.
 */
@Composable
private fun UploadBlockingDialog(uploadedCount: Int, totalCount: Int) {
    val c = LocalLumaColors.current
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(40.dp).clip(RoundedCornerShape(20.dp)).background(c.bg2)
                    .border(1.dp, c.hairline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(Modifier.size(34.dp), color = c.accent, strokeWidth = 3.dp)
                Spacer(Modifier.height(18.dp))
                Text(
                    "Saving to your vault",
                    fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.text,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (totalCount > 1) "Page ${(uploadedCount + 1).coerceAtMost(totalCount)} of $totalCount"
                    else "Uploading…",
                    fontFamily = LumaMono, fontSize = 12.sp, color = c.accentHi, letterSpacing = 0.4.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Keep this screen open until it finishes.",
                    fontFamily = LumaUi, fontSize = 12.sp, color = c.textMute,
                )
            }
        }
    }
}

/** Where the next page comes from: the photo library, or back to the camera (same as Retake). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddImageSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLumaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.bg2) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                "Add a page", fontFamily = LumaUi, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = c.text,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            AddImageOption(LumaIcons.Image, "Choose from gallery", "Pick photos already on this device", onGallery)
            Spacer(Modifier.height(10.dp))
            AddImageOption(LumaIcons.Camera, "Take a photo", "Open the camera and capture a new page", onCamera)
        }
    }
}

@Composable
private fun AddImageOption(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    val c = LocalLumaColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bg2)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.accentDim),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = c.accent, modifier = Modifier.size(19.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.text)
            Text(sub, fontFamily = LumaUi, fontSize = 12.sp, color = c.textMute, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(LumaIcons.Chevron, null, tint = c.textMute, modifier = Modifier.size(16.dp))
    }
}

/** Dashed "+" card sitting at the end of the page strip — same footprint as a real page. */
@Composable
private fun AddPageCard(onClick: () -> Unit) {
    val c = LocalLumaColors.current
    Column(
        Modifier.size(ThumbSize.MD.w, ThumbSize.MD.h).clip(RoundedCornerShape(10.dp))
            .background(c.accent.copy(alpha = 0.05f))
            .border(1.dp, c.accent.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(c.accentDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LumaIcons.Plus, null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Add Image", fontFamily = LumaUi, fontSize = 12.sp, color = c.accent)
    }
}

/**
 * A real thumbnail for a captured/picked file — the decoded image, or a PDF placeholder.
 * Takes the raw bytes (reference-equality, no content compare) so recompositions stay cheap.
 */
@Composable
private fun CapturedThumb(mimeType: String, bytes: ByteArray) {
    val c = LocalLumaColors.current
    val w = ThumbSize.MD.w
    val h = ThumbSize.MD.h
    if (mimeType.startsWith("image/")) {
        val bmp by produceState<ImageBitmap?>(null, bytes) {
            value = withContext(Dispatchers.Default) { decodeImageBitmap(bytes) }
        }
        Box(Modifier.size(w, h).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE8E2D4))) {
            bmp?.let {
                Image(
                    it,
                    null,
                    Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        Column(
            Modifier.size(w, h).clip(RoundedCornerShape(10.dp)).background(c.bg2)
                .border(1.dp, c.hairline, RoundedCornerShape(10.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(LumaIcons.Pdf, null, tint = c.accent, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "PDF",
                fontFamily = LumaMono,
                fontSize = 11.sp,
                color = c.textDim,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun CircleGlassButton(icon: ImageVector, onClick: () -> Unit) {
    val c = LocalLumaColors.current
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color(0x80000000))
            .border(1.dp, c.hairline2, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun FieldBox(content: @Composable () -> Unit) {
    val c = LocalLumaColors.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bg2)
            .border(1.dp, c.hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) { content() }
}

@Composable
private fun InlineField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLumaColors.current
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(
            placeholder,
            color = c.textMute,
            fontFamily = LumaUi,
            fontSize = 15.sp
        )
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(
                color = c.text,
                fontFamily = LumaUi,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth(),
        )
    }
}
