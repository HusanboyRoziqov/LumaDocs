package app.lumadocs.kmp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.IncomingBackupState
import app.lumadocs.kmp.screens.home.BackupPreviewItem
import app.lumadocs.kmp.screens.home.BackupPreviewScreen
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.viewmodels.BackupPhase
import app.lumadocs.kmp.viewmodels.SettingsViewModel
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.cancel
import lumadocs.composeapp.generated.resources.done
import lumadocs.composeapp.generated.resources.duplicates_skipped
import lumadocs.composeapp.generated.resources.files_added
import lumadocs.composeapp.generated.resources.files_uploaded
import lumadocs.composeapp.generated.resources.import_complete
import lumadocs.composeapp.generated.resources.import_count
import lumadocs.composeapp.generated.resources.import_failed
import lumadocs.composeapp.generated.resources.importing_backup
import lumadocs.composeapp.generated.resources.reading_backup
import lumadocs.composeapp.generated.resources.retry
import lumadocs.composeapp.generated.resources.select_files_to_import
import lumadocs.composeapp.generated.resources.unknown_error
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IncomingBackupImporter() {
    val backupUri by IncomingBackupState.uri.collectAsState()
    val uri = backupUri ?: return

    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(uri) {
        viewModel.prepareImport(uri)
    }

    // Full-screen preview: let the user choose which files to import
    if (state.backupPhase == BackupPhase.IMPORT_PREVIEW) {
        BackupPreviewScreen(
            title = stringResource(Res.string.select_files_to_import),
            items = state.importPreviewFiles.mapIndexed { index, meta ->
                BackupPreviewItem(
                    id = index.toString(),
                    name = meta.name,
                    mimeType = meta.mimeType,
                    category = meta.category
                )
            },
            confirmLabel = { count -> stringResource(Res.string.import_count, count) },
            onConfirm = { ids -> viewModel.importSelectedFiles(ids.map { it.toInt() }.toSet()) },
            onCancel = {
                viewModel.cancelBackupFlow()
                IncomingBackupState.clear()
            },
            loadBytes = { item -> viewModel.importFileBytes(item.id.toInt()) }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .background(nBlack400, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = nBrand100,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))

            when (state.backupPhase) {
                BackupPhase.IMPORT_LOADING -> {
                    Text(
                        stringResource(Res.string.reading_backup),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nWhite100
                    )
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = nBrand100, modifier = Modifier.size(40.dp))
                }

                BackupPhase.IMPORTING -> {
                    Text(
                        stringResource(Res.string.importing_backup),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nWhite100
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.files_uploaded, state.importProgress.toInt()),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = nBrand100,
                        trackColor = nBlack100
                    )
                }

                BackupPhase.SUCCESS -> {
                    val addedText = stringResource(Res.string.files_added, state.importedCount)
                    val skippedText = if (state.skippedCount > 0) "\n" + stringResource(
                        Res.string.duplicates_skipped,
                        state.skippedCount
                    ) else ""
                    Text(
                        stringResource(Res.string.import_complete),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nWhite100
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "$addedText$skippedText",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            viewModel.dismissBackupResult()
                            IncomingBackupState.clear()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = nBrand100),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(Res.string.done),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                BackupPhase.ERROR -> {
                    Text(
                        stringResource(Res.string.import_failed),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = nWhite100
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.backupError ?: stringResource(Res.string.unknown_error),
                        fontSize = 13.sp,
                        color = Color.Red
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.prepareImport(uri) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = nBrand100),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.retry), fontSize = 15.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.dismissBackupResult()
                                IncomingBackupState.clear()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = nBlack100),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(Res.string.cancel), fontSize = 15.sp)
                        }
                    }
                }

                else -> {
                    CircularProgressIndicator(color = nBrand100, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}
