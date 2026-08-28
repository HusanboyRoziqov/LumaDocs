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
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.action_continue
import lumadocs.composeapp.generated.resources.app_lock_sub
import lumadocs.composeapp.generated.resources.app_lock_title
import lumadocs.composeapp.generated.resources.app_version_note
import lumadocs.composeapp.generated.resources.badge_local
import lumadocs.composeapp.generated.resources.badge_synced
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.change_pin
import lumadocs.composeapp.generated.resources.disconnect
import lumadocs.composeapp.generated.resources.disconnect_drive_message
import lumadocs.composeapp.generated.resources.disconnect_drive_title
import lumadocs.composeapp.generated.resources.drive_connected_sub
import lumadocs.composeapp.generated.resources.drive_not_connected_sub
import lumadocs.composeapp.generated.resources.encryption
import lumadocs.composeapp.generated.resources.error
import lumadocs.composeapp.generated.resources.error_generic
import lumadocs.composeapp.generated.resources.ok
import lumadocs.composeapp.generated.resources.encryption_off
import lumadocs.composeapp.generated.resources.encryption_on
import lumadocs.composeapp.generated.resources.export_count
import lumadocs.composeapp.generated.resources.export_vault
import lumadocs.composeapp.generated.resources.export_vault_confirm_message
import lumadocs.composeapp.generated.resources.export_vault_confirm_title
import lumadocs.composeapp.generated.resources.export_vault_sub
import lumadocs.composeapp.generated.resources.exporting
import lumadocs.composeapp.generated.resources.google_drive
import lumadocs.composeapp.generated.resources.group_app
import lumadocs.composeapp.generated.resources.group_cloud_sync
import lumadocs.composeapp.generated.resources.group_security
import lumadocs.composeapp.generated.resources.guest
import lumadocs.composeapp.generated.resources.import_backup
import lumadocs.composeapp.generated.resources.import_backup_sub
import lumadocs.composeapp.generated.resources.import_count
import lumadocs.composeapp.generated.resources.importing_backup
import lumadocs.composeapp.generated.resources.language
import lumadocs.composeapp.generated.resources.loading_files
import lumadocs.composeapp.generated.resources.local_only
import lumadocs.composeapp.generated.resources.notifications
import lumadocs.composeapp.generated.resources.notifications_schedule
import lumadocs.composeapp.generated.resources.reading_backup
import lumadocs.composeapp.generated.resources.select_files_to_export
import lumadocs.composeapp.generated.resources.select_files_to_import
import lumadocs.composeapp.generated.resources.settings
import lumadocs.composeapp.generated.resources.settings_headline
import org.jetbrains.compose.resources.stringResource
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
    val encryptUploads by settingsViewModel.encryptNewUploads.collectAsStateWithLifecycle()

    var showExportConfirm by remember { mutableStateOf(false) }
    var showClearSessionsConfirm by remember { mutableStateOf(false) }
    val backupFilePicker = rememberBackupFilePicker { uriString ->
        if (uriString != null) settingsViewModel.prepareImport(uriString)
    }

    val connected = settingsState.isGoogleDriveConnected
    val user = settingsState.currentUser
    val name = user?.userName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.guest)
    val email = user?.userEmail?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.local_only)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(top = paddingValues.calculateTopPadding() + 8.dp, bottom = paddingValues.calculateBottomPadding() + 96.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
            Text(stringResource(Res.string.settings).uppercase(), fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 1.6.sp)
            Text(stringResource(Res.string.settings_headline), fontFamily = LumaDisplay, fontSize = 34.sp, letterSpacing = (-1).sp, color = c.text, modifier = Modifier.padding(top = 4.dp))
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
                Text(stringResource(if (connected) Res.string.badge_synced else Res.string.badge_local), fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = c.accentHi, letterSpacing = 0.4.sp)
            }
        }

        if (settingsState.error != null) {
            Text(settingsState.error!!, color = c.err, fontFamily = LumaUi, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }

        // Security
        SettingsGroup(stringResource(Res.string.group_security)) {
            SettingRow(LumaIcons.Face, stringResource(Res.string.app_lock_title), sub = stringResource(Res.string.app_lock_sub), trailing = {
                LumaToggle(on = appLockEnabled, onChange = { enabled ->
                    PinFlowState.start(if (enabled) PinFlowRequest.CREATE else PinFlowRequest.DISABLE)
                })
            })
            if (appLockEnabled) {
                SettingRow(LumaIcons.Key, stringResource(Res.string.change_pin), chevron = true, onClick = { PinFlowState.start(PinFlowRequest.CHANGE) })
            }
            // Applies to documents added from now on; anything already in the vault keeps the
            // state it was stored with.
            SettingRow(
                LumaIcons.Shield, stringResource(Res.string.encryption),
                sub = stringResource(if (encryptUploads) Res.string.encryption_on else Res.string.encryption_off),
                isLast = true,
                trailing = {
                    LumaToggle(on = encryptUploads, onChange = { settingsViewModel.setEncryptNewUploads(it) })
                },
            )
        }

        // Cloud & Sync
        SettingsGroup(stringResource(Res.string.group_cloud_sync)) {
            SettingRow(
                LumaIcons.Cloud, stringResource(Res.string.google_drive),
                sub = stringResource(if (connected) Res.string.drive_connected_sub else Res.string.drive_not_connected_sub),
                trailing = {
                    if (settingsState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
                    else LumaToggle(on = connected, onChange = { if (connected) showClearSessionsConfirm = true else settingsViewModel.onGoogleDriveToggle() })
                },
            )
            SettingRow(LumaIcons.Download, stringResource(Res.string.export_vault), sub = stringResource(Res.string.export_vault_sub), chevron = true, onClick = { showExportConfirm = true })
            SettingRow(LumaIcons.Folder, stringResource(Res.string.import_backup), sub = stringResource(Res.string.import_backup_sub), chevron = true, isLast = true, onClick = { backupFilePicker() })
        }

        // App
        SettingsGroup(stringResource(Res.string.group_app)) {
            SettingRow(LumaIcons.Globe, stringResource(Res.string.language), detail = if (languageCode == "ru") "Русский" else "English", chevron = true, onClick = {
                languageViewModel.switchLanguage(if (languageCode == "ru") "en" else "ru")
            })
            SettingRow(LumaIcons.Bell, stringResource(Res.string.notifications), detail = stringResource(Res.string.notifications_schedule), chevron = true, isLast = true)
        }

        Text(
            stringResource(Res.string.app_version_note),
            fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, letterSpacing = 0.4.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
    }

    // ── Backup / session dialogs (behavior preserved from the original) ──
    if (showExportConfirm) {
        LumaConfirm(
            title = stringResource(Res.string.export_vault_confirm_title),
            message = stringResource(Res.string.export_vault_confirm_message),
            confirm = stringResource(Res.string.action_continue), confirmColor = c.accent,
            onConfirm = { showExportConfirm = false; settingsViewModel.prepareExport() },
            onDismiss = { showExportConfirm = false },
        )
    }
    if (showClearSessionsConfirm) {
        LumaConfirm(
            title = stringResource(Res.string.disconnect_drive_title),
            message = stringResource(Res.string.disconnect_drive_message),
            confirm = stringResource(Res.string.disconnect), confirmColor = c.err,
            onConfirm = { showClearSessionsConfirm = false; settingsViewModel.clearAllSessions(); pinViewModel.disableLock() },
            onDismiss = { showClearSessionsConfirm = false },
        )
    }

    val loadingMessage = when (settingsState.backupPhase) {
        BackupPhase.EXPORT_LOADING -> stringResource(Res.string.loading_files)
        BackupPhase.EXPORTING -> stringResource(Res.string.exporting)
        BackupPhase.IMPORT_LOADING -> stringResource(Res.string.reading_backup)
        BackupPhase.IMPORTING -> stringResource(Res.string.importing_backup)
        else -> null
    }
    if (loadingMessage != null) BackupLoadingDialog(message = loadingMessage)

    // A failed export/import used to end in silence: the loading dialog vanished and the screen sat
    // in ERROR with the reason never shown. Surface it.
    if (settingsState.backupPhase == BackupPhase.ERROR) {
        val c2 = LocalLumaColors.current
        AlertDialog(
            onDismissRequest = { settingsViewModel.dismissBackupError() },
            containerColor = c2.bg2, titleContentColor = c2.text, textContentColor = c2.textDim,
            title = { Text(stringResource(Res.string.error), fontFamily = LumaUi, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    settingsState.backupError ?: stringResource(Res.string.error_generic),
                    fontFamily = LumaUi,
                )
            },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.dismissBackupError() }) {
                    Text(stringResource(Res.string.ok), color = c2.accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    if (settingsState.backupPhase == BackupPhase.EXPORT_PREVIEW) {
        Dialog(onDismissRequest = { settingsViewModel.cancelBackupFlow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackupPreviewScreen(
                title = stringResource(Res.string.select_files_to_export),
                items = settingsState.exportPreviewFiles.map { BackupPreviewItem(it.id, it.name, it.mimeType, it.category) },
                confirmLabel = { count -> stringResource(Res.string.export_count, count) },
                onConfirm = { ids -> settingsViewModel.exportSelectedFiles(ids) },
                onCancel = { settingsViewModel.cancelBackupFlow() },
                loadBytes = { item -> settingsViewModel.downloadExportFile(item.id) },
            )
        }
    }
    if (settingsState.backupPhase == BackupPhase.IMPORT_PREVIEW) {
        Dialog(onDismissRequest = { settingsViewModel.cancelBackupFlow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackupPreviewScreen(
                title = stringResource(Res.string.select_files_to_import),
                items = settingsState.importPreviewFiles.mapIndexed { i, meta -> BackupPreviewItem(i.toString(), meta.name, meta.mimeType, meta.category) },
                confirmLabel = { count -> stringResource(Res.string.import_count, count) },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = c.textDim) } },
    )
}
