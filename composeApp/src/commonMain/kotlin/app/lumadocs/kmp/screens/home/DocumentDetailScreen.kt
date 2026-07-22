package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.rememberNotificationPermissionLauncher
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.theme.nIconChip
import app.lumadocs.kmp.theme.nWhite100
import coil3.compose.AsyncImage
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.delete
import lumadocs.composeapp.generated.resources.delete_confirm_message
import lumadocs.composeapp.generated.resources.delete_confirm_title
import lumadocs.composeapp.generated.resources.field_description_label
import lumadocs.composeapp.generated.resources.field_name
import lumadocs.composeapp.generated.resources.ok
import lumadocs.composeapp.generated.resources.save
import lumadocs.composeapp.generated.resources.section_expiry_date
import lumadocs.composeapp.generated.resources.tap_to_pick_date
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailScreen(
    file: DriveFile,
    previewBitmap: ImageBitmap?,
    isLoadingPreview: Boolean = false,
    isSaving: Boolean = false,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSave: (newTitle: String, newDescription: String?, newExpiryDate: String?) -> Unit,
    previewOverride: (@Composable () -> Unit)? = null,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf<String?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionLauncher { }
    LaunchedEffect(file.id) {
        title = file.name
        description = file.description ?: ""
        expiryDate = file.expiryDate?.ifEmpty { null }
    }

    val mimeTypeLabel = remember(file.mimeType) { getMimeTypeLabel(file.mimeType) }
    val isImage = mimeTypeLabel == "Image"

    val previewRatio = previewBitmap?.let {
        (it.width.toFloat() / it.height.toFloat()).coerceIn(0.7f, 1.7f)
    }
    val thumbModel = remember(file.thumbnailLink) {
        if (isOnline()) file.thumbnailLink?.replace(Regex("=s\\d+"), "=s1200")
        else file.thumbnailLink
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = nBlack400,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = nBlack300
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (previewRatio != null) Modifier.aspectRatio(previewRatio)
                    else Modifier.height(280.dp)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(nBlack400)
                .then(if (isLoadingPreview && previewBitmap == null) Modifier.shimmerEffect() else Modifier)
                .then(
                    if (previewOverride == null) Modifier.clickable {
                        if (isImage) showFullscreen = true else onOpen()
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewOverride != null) {
                previewOverride()
            } else {
                when {
                    previewBitmap != null -> Image(
                        bitmap = previewBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    isLoadingPreview -> {

                    }

                    file.thumbnailLink != null -> AsyncImage(
                        model = thumbModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    else -> Icon(
                        imageVector = fileTypeIcon(mimeTypeLabel),
                        contentDescription = null,
                        tint = nBlack300,
                        modifier = Modifier.size(72.dp)
                    )
                }

                if (file.encrypted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(30.dp)
                            .background(nBrand100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = nWhite100,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    onClick = { if (isImage) showFullscreen = true else onOpen() },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isImage) Icons.Filled.Fullscreen else Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "Open",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (file.encrypted) MetaIconChip(icon = Icons.Filled.Lock, tint = nBrand100)

            MetaChip(icon = fileTypeIcon(mimeTypeLabel), label = mimeTypeLabel)

            formatFileSize(file.size)?.let { MetaChip(icon = Icons.Filled.Save, label = it) }

            expiryDate?.let { expiry ->
                val urgency = remember(expiry) { expiryUrgency(expiry) }
                val color = when (urgency) {
                    ExpiryUrgency.EXPIRED, ExpiryUrgency.TODAY -> Color(0xFFFF3B30)
                    ExpiryUrgency.SOON -> Color(0xFFFF9500)
                    ExpiryUrgency.UPCOMING -> Color(0xFF34C759)
                }
                ExpiryChip(label = expiry, color = color)
            }
        }

        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(Res.string.field_name))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = nWhite100, fontSize = 15.sp),
            colors = darkDetailFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel(stringResource(Res.string.field_description_label))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth().height(130.dp),
            singleLine = false,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = nWhite100, fontSize = 15.sp),
            colors = darkDetailFieldColors()
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel(stringResource(Res.string.section_expiry_date))
        Spacer(Modifier.height(8.dp))
        // The date saved on the server (set at upload time / last save).
        val serverExpiry = file.expiryDate?.ifEmpty { null }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(nBlack400)
                    .border(
                        width = 1.dp,
                        color = if (expiryDate != null) nBrand100 else nBlack300.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = if (expiryDate != null) nBrand100 else nBlack300,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = expiryDate ?: stringResource(Res.string.tap_to_pick_date),
                        fontSize = 15.sp,
                        color = if (expiryDate != null) nWhite100 else nBlack300,
                        fontWeight = if (expiryDate != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            // Revert button: only visible once the date differs from the saved server value.
            // Tapping it restores the expiry date that was set at upload time.
            if (expiryDate != serverExpiry) {
                Surface(
                    onClick = { expiryDate = serverExpiry },
                    color = nBlack400,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Revert to saved date",
                            tint = nBrand100,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        val hasChanges = title.trim() != file.name.trim() ||
                description.trim() != (file.description ?: "").trim() ||
                expiryDate != file.expiryDate?.ifEmpty { null }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasChanges) {
                Button(
                    onClick = {
                        val descToSave = description.trim().ifEmpty { null }
                        onSave(title.trim(), descToSave, expiryDate)
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nBrand100,
                        contentColor = nWhite100,
                        disabledContainerColor = nBrand100.copy(alpha = 0.6f),
                        disabledContentColor = nWhite100
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = nWhite100,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.save),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            ActionSquare(
                bg = nIconChip,
                onClick = onShare
            ) {
                Icon(Icons.Filled.Share, "Share", tint = nWhite100, modifier = Modifier.size(22.dp))
            }

            ActionSquare(
                bg = Color(0xFFFF3B30).copy(alpha = 0.14f),
                onClick = { showDeleteConfirm = true }
            ) {
                Icon(
                    Icons.Filled.Delete,
                    "Delete",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = nBlack400,
            titleContentColor = nWhite100,
            textContentColor = nBlack300,
            title = {
                Text(
                    stringResource(Res.string.delete_confirm_title),
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text(stringResource(Res.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        stringResource(Res.string.delete),
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel), color = nBlack300)
                }
            }
        )
    }

    if (showFullscreen && isImage) {
        FullscreenImageViewer(
            bitmap = previewBitmap,
            thumbnailLink = thumbModel,
            onDismiss = { showFullscreen = false }
        )
    }

    if (showDatePicker) {
        val todayUtcMillis = remember { startOfTodayUtcMillis() }
        val currentYear = remember {
            kotlinx.datetime.Instant.fromEpochMilliseconds(todayUtcMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).year
        }
        // Preselect the existing expiry when it is still in the future, otherwise start on today.
        val initialMillis = remember(expiryDate) {
            (expiryDate?.let { isoDateToUtcMillis(it) }?.takeIf { it >= todayUtcMillis })
                ?: todayUtcMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = currentYear..(currentYear + 20),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year >= currentYear
            }
        )
        val darkDatePickerColors = DatePickerDefaults.colors(
            containerColor = nBlack400,
            titleContentColor = nBlack300,
            headlineContentColor = nWhite100,
            weekdayContentColor = nBlack300,
            subheadContentColor = nBlack300,
            navigationContentColor = nWhite100,
            yearContentColor = nWhite100,
            currentYearContentColor = nBrand100,
            selectedYearContentColor = nWhite100,
            selectedYearContainerColor = nBrand100,
            dayContentColor = nWhite100,
            disabledDayContentColor = nBlack300.copy(alpha = 0.35f),
            selectedDayContentColor = nWhite100,
            selectedDayContainerColor = nBrand100,
            disabledSelectedDayContentColor = nBlack300.copy(alpha = 0.35f),
            todayContentColor = nBrand100,
            todayDateBorderColor = nBrand100,
            dividerColor = nBlack100,
            dateTextFieldColors = TextFieldDefaults.colors(
                focusedTextColor = nWhite100,
                unfocusedTextColor = nWhite100,
                focusedContainerColor = nBlack100,
                unfocusedContainerColor = nBlack100,
                cursorColor = nBrand100,
                focusedIndicatorColor = nBrand100,
                unfocusedIndicatorColor = nBlack300,
                focusedLabelColor = nBrand100,
                unfocusedLabelColor = nBlack300,
                focusedPlaceholderColor = nBlack300,
                unfocusedPlaceholderColor = nBlack300,
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expiryDate = epochMillisToIsoDate(millis)
                        requestNotificationPermission()
                    }
                    showDatePicker = false
                }) {
                    Text(
                        stringResource(Res.string.ok),
                        color = nBrand100,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel), color = nBlack300)
                }
            },
            colors = darkDatePickerColors
        ) {
            DatePicker(state = datePickerState, colors = darkDatePickerColors)
        }
    }
}

@Composable
internal fun FullscreenImageViewer(
    bitmap: ImageBitmap?,
    thumbnailLink: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offsetX += panChange.x
                offsetY += panChange.y
            } else {
                offsetX = 0f
                offsetY = 0f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(nBlack100)
                .clickable(enabled = scale <= 1f) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val imageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .transformable(transformState)

            when {
                bitmap != null -> Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )

                thumbnailLink != null -> AsyncImage(
                    model = thumbnailLink,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            }

            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = nWhite100.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = nWhite100,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = nBlack300,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
internal fun MetaChip(
    icon: ImageVector,
    label: String,
    tint: Color = nWhite100,
    background: Color = nBlack400,
) {
    Surface(color = background, shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Text(
                label,
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Icon-only variant of [MetaChip] — used for the encryption indicator (no text label). */
@Composable
internal fun MetaIconChip(icon: ImageVector, tint: Color = nWhite100) {
    Surface(color = nBlack400, shape = RoundedCornerShape(10.dp)) {
        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = "Encrypted",
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun ExpiryChip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Filled.Timer, null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionSquare(bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun darkDetailFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = nBrand100,
    unfocusedBorderColor = nBlack300.copy(alpha = 0.4f),
    focusedTextColor = nWhite100,
    unfocusedTextColor = nWhite100,
    cursorColor = nBrand100,
    focusedContainerColor = nBlack400,
    unfocusedContainerColor = nBlack400,
)

internal fun formatFileSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val kb = bytes / 1024.0
    return if (kb < 1024) "${kb.roundToInt()} KB"
    else "${((kb / 1024) * 10).roundToInt() / 10.0} MB"
}

private fun epochMillisToIsoDate(millis: Long): String {
    return kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
        .toString()
}

private fun isoDateToUtcMillis(iso: String): Long? {
    return try {
        kotlinx.datetime.LocalDate.parse(iso)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
            .toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun startOfTodayUtcMillis(): Long {
    return kotlin.time.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
        .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
        .toEpochMilliseconds()
}
