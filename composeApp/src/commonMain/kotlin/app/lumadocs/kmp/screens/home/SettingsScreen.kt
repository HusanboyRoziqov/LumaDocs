package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lumadocs.kmp.LanguageViewModel
import app.lumadocs.kmp.PinFlowRequest
import app.lumadocs.kmp.PinFlowState
import app.lumadocs.kmp.PinViewModel
import app.lumadocs.kmp.data_store.rememberDataStore
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaDisplay
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.InitialAvatar
import app.lumadocs.kmp.ui.LumaToggle
import app.lumadocs.kmp.ui.SettingRow
import app.lumadocs.kmp.ui.SettingsGroup
import app.lumadocs.kmp.viewmodels.BackupPhase
import app.lumadocs.kmp.viewmodels.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    val c = LocalLumaColors.current
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val dataStore = rememberDataStore()
    val languageViewModel = viewModel { LanguageViewModel(dataStore) }
    val languageCode by languageViewModel.languageCode.collectAsStateWithLifecycle()
    val pinViewModel = viewModel { PinViewModel(dataStore) }
    val appLockEnabled by pinViewModel.lockEnabled.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var showExportConfirm by remember { mutableStateOf(false) }
    var showClearSessionsConfirm by remember { mutableStateOf(false) }
    val backupFilePicker = rememberBackupFilePicker { uriString ->
        if (uriString != null) settingsViewModel.prepareImport(uriString)
    }

    val connected = settingsState.isGoogleDriveConnected
    val user = settingsState.currentUser
    val name = user?.userName?.takeIf { it.isNotBlank() } ?: "Guest"
    val email = user?.userEmail?.takeIf { it.isNotBlank() } ?: "Local only · not signed in"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding() + 8.dp, bottom = paddingValues.calculateBottomPadding() + 96.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
            Text("SETTINGS", fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.6.sp)
            Text("Your vault.", fontFamily = LumaDisplay, fontSize = 34.sp, letterSpacing = (-1).sp, color = c.text, modifier = Modifier.padding(top = 4.dp))
        }

        // Account card
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(18.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InitialAvatar(letter = name, sizeDp = 52.dp, fontSize = 22)
            Column(Modifier.weight(1f)) {
                Text(name, fontFamily = LumaUi, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.text)
                Text(email, fontFamily = LumaMono, fontSize = 12.sp, color = c.textMute, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(c.accentDim).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(if (connected) "SYNCED" else "LOCAL", fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = c.accentHi, letterSpacing = 0.4.sp)
            }
        }

        if (settingsState.error != null) {
            Text(settingsState.error!!, color = c.err, fontFamily = LumaUi, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }

        // Security
        SettingsGroup("Security") {
            SettingRow(LumaIcons.Face, "App Lock", sub = "Unlock the vault with PIN / biometrics", trailing = {
                LumaToggle(on = appLockEnabled, onChange = { enabled ->
                    PinFlowState.start(if (enabled) PinFlowRequest.CREATE else PinFlowRequest.DISABLE)
                })
            })
            if (appLockEnabled) {
                SettingRow(LumaIcons.Key, "Change PIN", chevron = true, onClick = { PinFlowState.start(PinFlowRequest.CHANGE) })
            }
            SettingRow(LumaIcons.Shield, "Encryption", sub = "AES-256 · Active", detail = "Active", chevron = false, isLast = true)
        }

        // Cloud & Sync
        SettingsGroup("Cloud & Sync") {
            SettingRow(
                LumaIcons.Cloud, "Google Drive",
                sub = if (connected) "Encrypted · connected" else "Not connected",
                trailing = {
                    if (settingsState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
                    else LumaToggle(on = connected, onChange = { if (connected) showClearSessionsConfirm = true else settingsViewModel.onGoogleDriveToggle() })
                },
            )
            SettingRow(LumaIcons.Download, "Export vault archive", sub = "Encrypted .lumavault file", chevron = true, onClick = { showExportConfirm = true })
            SettingRow(LumaIcons.Folder, "Import backup", sub = "Restore from a .lumavault file", chevron = true, isLast = true, onClick = { backupFilePicker() })
        }

        // App
        SettingsGroup("App") {
            SettingRow(LumaIcons.Globe, "Language", detail = if (languageCode == "ru") "Русский" else "English", chevron = true, onClick = {
                languageViewModel.switchLanguage(if (languageCode == "ru") "en" else "ru")
            })
            SettingRow(LumaIcons.Bell, "Notifications", detail = "90 · 30 · 7 · 1 day", chevron = true)
            SettingRow(LumaIcons.Sparkle, "OCR & Smart Search", detail = "On", chevron = true, isLast = true)
        }

        Text(
            "LumaDocs v1.0 · Build 2026.4.18\nMade privately. Encrypted always.",
            fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, letterSpacing = 0.4.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
    }

    // ── Backup / session dialogs (behavior preserved from the original) ──
    if (showExportConfirm) {
        LumaConfirm(
            title = "Export vault archive?",
            message = "Choose which documents to include in an encrypted backup file.",
            confirm = "Continue", confirmColor = c.accent,
            onConfirm = { showExportConfirm = false; settingsViewModel.prepareExport() },
            onDismiss = { showExportConfirm = false },
        )
    }
    if (showClearSessionsConfirm) {
        LumaConfirm(
            title = "Disconnect Google Drive?",
            message = "This signs you out and clears every cached file and the app lock on this device.",
            confirm = "Disconnect", confirmColor = c.err,
            onConfirm = { showClearSessionsConfirm = false; settingsViewModel.clearAllSessions(); pinViewModel.disableLock() },
            onDismiss = { showClearSessionsConfirm = false },
        )
    }

    val loadingMessage = when (settingsState.backupPhase) {
        BackupPhase.EXPORT_LOADING -> "Loading files…"
        BackupPhase.EXPORTING -> "Exporting…"
        BackupPhase.IMPORT_LOADING -> "Reading backup…"
        BackupPhase.IMPORTING -> "Importing backup…"
        else -> null
    }
    if (loadingMessage != null) BackupLoadingDialog(message = loadingMessage)

    if (settingsState.backupPhase == BackupPhase.EXPORT_PREVIEW) {
        Dialog(onDismissRequest = { settingsViewModel.cancelBackupFlow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackupPreviewScreen(
                title = "Select files to export",
                items = settingsState.exportPreviewFiles.map { BackupPreviewItem(it.id, it.name, it.mimeType, it.category) },
                confirmLabel = { count -> "Export $count" },
                onConfirm = { ids -> settingsViewModel.exportSelectedFiles(ids) },
                onCancel = { settingsViewModel.cancelBackupFlow() },
                loadBytes = { item -> settingsViewModel.downloadExportFile(item.id) },
            )
        }
    }
    if (settingsState.backupPhase == BackupPhase.IMPORT_PREVIEW) {
        Dialog(onDismissRequest = { settingsViewModel.cancelBackupFlow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackupPreviewScreen(
                title = "Select files to import",
                items = settingsState.importPreviewFiles.mapIndexed { i, meta -> BackupPreviewItem(i.toString(), meta.name, meta.mimeType, meta.category) },
                confirmLabel = { count -> "Import $count" },
                onConfirm = { ids -> settingsViewModel.importSelectedFiles(ids.map { it.toInt() }.toSet()) },
                onCancel = { settingsViewModel.cancelBackupFlow() },
                loadBytes = { item -> settingsViewModel.importFileBytes(item.id.toInt()) },
            )
        }
    }
}

@Composable
private fun LumaConfirm(
    title: String,
    message: String,
    confirm: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLumaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bg2, titleContentColor = c.text, textContentColor = c.textDim,
        title = { Text(title, fontFamily = LumaUi, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, fontFamily = LumaUi) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = confirmColor, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textDim) } },
    )
}
