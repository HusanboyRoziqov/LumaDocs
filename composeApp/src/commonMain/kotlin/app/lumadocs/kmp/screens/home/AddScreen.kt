package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lumadocs.kmp.icons.GoogleDriveIcon
import app.lumadocs.kmp.platform.PickedFile
import app.lumadocs.kmp.platform.hasGalleryPermission
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.platform.readGalleryImage
import app.lumadocs.kmp.platform.rememberCameraCapture
import app.lumadocs.kmp.platform.rememberFilePicker
import app.lumadocs.kmp.platform.rememberGalleryPermissionRequest
import app.lumadocs.kmp.platform.rememberNotificationPermissionLauncher
import app.lumadocs.kmp.platform.showToast
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.theme.nIconChip
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nWhite600
import app.lumadocs.kmp.utils.ImagePickerState
import app.lumadocs.kmp.utils.decodeImageBitmap
import app.lumadocs.kmp.utils.observeImagePickerState
import app.lumadocs.kmp.viewmodels.AddScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.add_document
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.connect_drive_message
import lumadocs.composeapp.generated.resources.connect_drive_title
import lumadocs.composeapp.generated.resources.document_desc
import lumadocs.composeapp.generated.resources.document_title
import lumadocs.composeapp.generated.resources.encrypt_files
import lumadocs.composeapp.generated.resources.encrypt_subtitle
import lumadocs.composeapp.generated.resources.error_folder_required
import lumadocs.composeapp.generated.resources.error_name_required
import lumadocs.composeapp.generated.resources.expiry_optional_hint
import lumadocs.composeapp.generated.resources.field_description_label
import lumadocs.composeapp.generated.resources.field_title
import lumadocs.composeapp.generated.resources.folder_name
import lumadocs.composeapp.generated.resources.folder_name_hint
import lumadocs.composeapp.generated.resources.go_to_settings
import lumadocs.composeapp.generated.resources.loading_files
import lumadocs.composeapp.generated.resources.no_internet
import lumadocs.composeapp.generated.resources.ok
import lumadocs.composeapp.generated.resources.photos_count
import lumadocs.composeapp.generated.resources.reminders_info
import lumadocs.composeapp.generated.resources.section_details
import lumadocs.composeapp.generated.resources.section_expiry_date
import lumadocs.composeapp.generated.resources.section_options
import lumadocs.composeapp.generated.resources.select_image
import lumadocs.composeapp.generated.resources.set_expiry_date
import lumadocs.composeapp.generated.resources.tap_to_browse_photos
import lumadocs.composeapp.generated.resources.tap_to_pick_date
import lumadocs.composeapp.generated.resources.upload_document
import lumadocs.composeapp.generated.resources.uploading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AddScreen(
    modifier: Modifier = Modifier,
    imagePickerLambda: (() -> Unit)? = null,
    onNavigateToSettings: () -> Unit = {},
) {
    val viewModel: AddScreenViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (uiState.needsGoogleDriveConnection) {
        ConnectDrivePrompt(
            onGoToSettings = {
                viewModel.dismissConnectPrompt()
                onNavigateToSettings()
            },
            onDismiss = { viewModel.dismissConnectPrompt() },
        )
    }

    var customFileName by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var folderError by remember { mutableStateOf(false) }
    var imageDesc by remember { mutableStateOf("") }
    var makeEncrypted by remember { mutableStateOf(false) }

    val hasNonImage = uiState.selectedFiles.any { !it.mimeType.startsWith("image/") }
    LaunchedEffect(hasNonImage) {
        if (hasNonImage && makeEncrypted) {
            makeEncrypted = false
            viewModel.setEncryptionForAll(false)
        }
    }

    var expiryEnabled by remember { mutableStateOf(false) }
    var expiryDateIso by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionLauncher { }

    var isClickAllowed by remember { mutableStateOf(true) }
    val pickerState = observeImagePickerState()

    LaunchedEffect(pickerState.bytes) {
        val bytes = pickerState.bytes ?: return@LaunchedEffect
        val name = pickerState.fileName ?: return@LaunchedEffect
        viewModel.addFileToList(name, pickerState.mimeType ?: "image/jpeg", bytes)
        ImagePickerState.clearSelection()
    }

    var showPickerSheet by remember { mutableStateOf(false) }
    var galleryPermission by remember { mutableStateOf(hasGalleryPermission()) }
    var isAddingSelection by remember { mutableStateOf(false) }
    var editorImages by remember { mutableStateOf<List<PickedFile>?>(null) }

    val filePicker = rememberFilePicker { picked ->
        picked.forEach { viewModel.addFileToList(it.fileName, it.mimeType, it.bytes) }
        showPickerSheet = false
    }
    val cameraCapture = rememberCameraCapture { photo ->
        showPickerSheet = false
        if (photo != null) editorImages = listOf(photo)
    }
    val requestGalleryPermission = rememberGalleryPermissionRequest { granted ->
        galleryPermission = granted
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(1000)
            customFileName = ""
            folderName = ""
            imageDesc = ""
            makeEncrypted = false
            expiryEnabled = false
            expiryDateIso = null
            viewModel.clearMessages()
            viewModel.clearSelectedFiles()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nBlack100)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.add_document),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = nWhite100
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = nBlack100,
                titleContentColor = nWhite100
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            if (uiState.selectedFiles.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.photos_count, uiState.selectedFiles.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = nBlack300,
                    letterSpacing = 0.8.sp
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((uiState.selectedFiles.size / 3 + 1) * 130).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.selectedFiles) { index, file ->
                        ImageThumbnail(
                            fileBytes = file.fileBytes,
                            fileName = file.fileName,
                            mimeType = file.mimeType,
                            onRemove = { viewModel.removeFileFromList(index) }
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(nBlack400)
                                .border(
                                    1.dp,
                                    nBlack300.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { showPickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "+",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = nBrand100
                                )
                                Text("Add", fontSize = 10.sp, color = nBlack300)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(nBlack400)
                        .border(
                            width = 1.5.dp,
                            brush = SolidColor(nBrand100.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { showPickerSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = nBrand100,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(Res.string.select_image),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = nWhite100
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.tap_to_browse_photos),
                            fontSize = 12.sp,
                            color = nBlack300
                        )
                    }
                }
            }

            AddSectionLabel(stringResource(Res.string.section_details))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = nBlack400),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AddOutlinedField(
                        value = customFileName,
                        onValueChange = { customFileName = it; nameError = false },
                        placeholder = stringResource(Res.string.document_title),
                        label = stringResource(Res.string.field_title),
                        errorText = if (nameError) stringResource(Res.string.error_name_required) else null
                    )
                    AddOutlinedField(
                        value = imageDesc,
                        onValueChange = {
                            imageDesc = it
                            viewModel.updateFileDescription(it)
                        },
                        placeholder = stringResource(Res.string.document_desc),
                        label = stringResource(Res.string.field_description_label)
                    )
                    if (uiState.selectedFiles.size > 1) {
                        AddOutlinedField(
                            value = folderName,
                            onValueChange = { folderName = it; folderError = false },
                            placeholder = stringResource(Res.string.folder_name_hint),
                            label = stringResource(Res.string.folder_name),
                            errorText = if (folderError) stringResource(Res.string.error_folder_required) else null
                        )
                    }
                }
            }

            if (!hasNonImage) {
                AddSectionLabel(stringResource(Res.string.section_options))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = nBlack400),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(nIconChip, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = nWhite100,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(Res.string.encrypt_files),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = nWhite100
                                )
                                Text(
                                    stringResource(Res.string.encrypt_subtitle),
                                    fontSize = 12.sp,
                                    color = nBlack300
                                )
                            }
                            Switch(
                                checked = makeEncrypted,
                                onCheckedChange = {
                                    makeEncrypted = it
                                    viewModel.setEncryptionForAll(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = nWhite100,
                                    checkedTrackColor = nBrand100,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = nBlack100
                                )
                            )
                        }

                    }
                }
            }

            AddSectionLabel(stringResource(Res.string.section_expiry_date))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = nBlack400),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(nIconChip, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = nWhite100,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.set_expiry_date),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = nWhite100
                            )
                            Text(
                                stringResource(Res.string.expiry_optional_hint),
                                fontSize = 12.sp,
                                color = nBlack300
                            )
                        }
                        Switch(
                            checked = expiryEnabled,
                            onCheckedChange = {
                                expiryEnabled = it
                                if (it) requestNotificationPermission()
                                else expiryDateIso = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = nWhite100,
                                checkedTrackColor = nBrand100,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = nBlack100
                            )
                        )
                    }

                    if (expiryEnabled) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(nBlack100)
                                .border(
                                    width = 1.dp,
                                    color = if (expiryDateIso != null) nBrand100 else nBlack300.copy(
                                        alpha = 0.3f
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = expiryDateIso ?: stringResource(Res.string.tap_to_pick_date),
                                fontSize = 15.sp,
                                color = if (expiryDateIso != null) nWhite100 else nBlack300,
                                fontWeight = if (expiryDateIso != null) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().padding(end = 32.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = nBlack300,
                                modifier = Modifier.align(Alignment.CenterEnd).size(18.dp)
                            )
                        }

                        if (expiryDateIso != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.reminders_info),
                                fontSize = 11.sp,
                                color = nBlack300,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = nBrand100,
                    trackColor = nBlack400
                )
            }

            if (uiState.successMessage != null) {
                AddStatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    text = "${uiState.successMessage}",
                    textColor = Color(0xFF4CAF50),
                    bgColor = Color(0xFF4CAF50).copy(alpha = 0.10f)
                )
            }

            if (uiState.errorMessage != null) {
                AddStatusBanner(
                    icon = Icons.Filled.Error,
                    text = "${uiState.errorMessage}",
                    textColor = Color(0xFFFF6B6B),
                    bgColor = Color(0xFFFF6B6B).copy(alpha = 0.10f)
                )
            }

            val isMultiple = uiState.selectedFiles.size > 1
            val noInternetMsg = stringResource(Res.string.no_internet)
            // The upload button only appears once at least one photo/file has been picked.
            if (uiState.selectedFiles.isNotEmpty()) {
                Button(
                    onClick = {
                        // Validate required fields; description stays optional.
                        nameError = customFileName.isBlank()
                        folderError = isMultiple && folderName.isBlank()
                        if (!nameError && !folderError) {
                            if (!isOnline()) {
                                showToast(noInternetMsg)
                            } else {
                                viewModel.uploadMultipleFiles(
                                    title = customFileName,
                                    folderName = folderName,
                                    expiryDate = if (expiryEnabled) expiryDateIso else null
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nBrand100,
                        contentColor = nWhite100,
                        disabledContainerColor = nBlack400,
                        disabledContentColor = nBlack300
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (uiState.isLoading) stringResource(Res.string.uploading)
                        else stringResource(Res.string.upload_document),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showPickerSheet) {
        GalleryPickerSheet(
            hasPermission = galleryPermission,
            onRequestPermission = { requestGalleryPermission() },
            onPickFiles = { filePicker() },
            onCameraClick = { cameraCapture() },
            onConfirmImages = { uris ->
                coroutineScope.launch {
                    isAddingSelection = true
                    val picked = uris.mapNotNull { readGalleryImage(it) }
                    isAddingSelection = false
                    showPickerSheet = false
                    if (picked.isNotEmpty()) editorImages = picked
                }
            },
            onDismiss = { showPickerSheet = false }
        )
    }

    editorImages?.let { imgs ->
        Dialog(
            onDismissRequest = { editorImages = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MediaEditorScreen(
                initial = imgs,
                onSend = { finalList ->
                    finalList.forEach {
                        viewModel.addFileToList(
                            it.fileName,
                            it.mimeType,
                            it.bytes
                        )
                    }
                    editorImages = null
                },
                onCancel = { editorImages = null }
            )
        }
    }

    if (isAddingSelection) {
        BackupLoadingDialog(message = stringResource(Res.string.loading_files))
    }

    if (showDatePicker) {

        val todayUtcMillis = remember { startOfTodayUtcMillis() }
        val currentYear = remember {
            kotlinx.datetime.Instant.fromEpochMilliseconds(todayUtcMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).year
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = todayUtcMillis,
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
                        expiryDateIso = epochMillisToIsoDate(millis)
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
private fun AddSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = nBlack300,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    errorText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = nBlack300) },
        label = { Text(label, color = nBlack300) },
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let {
            { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = nBrand100,
            unfocusedBorderColor = nBlack300.copy(alpha = 0.4f),
            focusedLabelColor = nBrand100,
            unfocusedLabelColor = nBlack300,
            focusedTextColor = nWhite100,
            unfocusedTextColor = nWhite100,
            cursorColor = nBrand100,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorBorderColor = Color(0xFFFF6B6B),
            errorLabelColor = Color(0xFFFF6B6B),
            errorCursorColor = Color(0xFFFF6B6B),
            errorContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun AddStatusBanner(icon: ImageVector, text: String, textColor: Color, bgColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
    }
}

@Composable
private fun ConnectDrivePrompt(onGoToSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = nBlack400,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(nBrand100.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GoogleDriveIcon,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        title = {
            Text(
                text = stringResource(Res.string.connect_drive_title),
                color = nWhite100,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.connect_drive_message),
                color = nWhite600,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onGoToSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = nBrand100,
                    contentColor = nWhite100,
                ),
            ) {
                Text(stringResource(Res.string.go_to_settings), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = nWhite600)
            }
        },
    )
}

@Composable
private fun ImageThumbnail(
    fileBytes: ByteArray,
    fileName: String,
    mimeType: String,
    onRemove: () -> Unit,
) {
    val isImage = mimeType.startsWith("image/")
    val imageBitmap = remember(fileBytes) { if (isImage) decodeImageBitmap(fileBytes) else null }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(nBlack400)
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = fileTypeIcon(getMimeTypeLabel(mimeType)),
                    contentDescription = null,
                    tint = nBrand100,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = fileName,
                    fontSize = 9.sp,
                    color = nBlack300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .size(22.dp)
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove",
                tint = nWhite100,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun epochMillisToIsoDate(millis: Long): String {
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
    return date.toString()
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun startOfTodayUtcMillis(): Long {
    val todayUtc = kotlin.time.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
    return todayUtc.atStartOfDayIn(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
}

@Composable
internal expect fun setupImagePicker(): (() -> Unit)?

internal expect fun getSelectedFileName(): String?

internal expect fun getSelectedImageBytes(): ByteArray?

internal expect fun getCurrentImageMimeType(): String
