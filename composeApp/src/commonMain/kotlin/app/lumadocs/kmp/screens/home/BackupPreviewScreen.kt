package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.lumadocs.kmp.IncomingFile
import app.lumadocs.kmp.platform.openFileExternally
import app.lumadocs.kmp.screens.IncomingFileViewer
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import kotlinx.coroutines.launch
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.close
import lumadocs.composeapp.generated.resources.deselect_all
import lumadocs.composeapp.generated.resources.files_selected
import lumadocs.composeapp.generated.resources.opening_file
import lumadocs.composeapp.generated.resources.select_all
import org.jetbrains.compose.resources.stringResource

data class BackupPreviewItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val category: String?,
)

@Composable
internal fun BackupPreviewScreen(
    title: String,
    items: List<BackupPreviewItem>,
    confirmLabel: @Composable (selectedCount: Int) -> String,
    onConfirm: (selectedIds: Set<String>) -> Unit,
    onCancel: () -> Unit,
    loadBytes: suspend (BackupPreviewItem) -> ByteArray?,
) {
    var selected by remember(items) { mutableStateOf(items.map { it.id }.toSet()) }
    val allSelected = selected.size == items.size && items.isNotEmpty()

    val scope = rememberCoroutineScope()
    var viewingFile by remember { mutableStateOf<IncomingFile?>(null) }
    var isOpening by remember { mutableStateOf(false) }

    fun openItem(item: BackupPreviewItem) {
        if (isOpening) return
        scope.launch {
            isOpening = true
            try {
                val bytes = loadBytes(item) ?: return@launch
                if (item.mimeType.startsWith("image/")) {
                    viewingFile =
                        IncomingFile(bytes = bytes, fileName = item.name, mimeType = item.mimeType)
                } else {
                    openFileExternally(bytes, item.name, item.mimeType)
                }
            } finally {
                isOpening = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(nBlack100)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = nWhite100,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = onCancel,
                    shape = CircleShape,
                    color = nBlack400,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.close),
                            tint = nBlack300,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.files_selected, selected.size, items.size),
                    fontSize = 13.sp,
                    color = nBlack300,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = {
                        selected = if (allSelected) emptySet() else items.map { it.id }.toSet()
                    },
                    shape = RoundedCornerShape(50),
                    color = nBlack400
                ) {
                    Text(
                        text = stringResource(if (allSelected) Res.string.deselect_all else Res.string.select_all),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = nBrand100,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val isChecked = item.id in selected
                    PreviewFileRow(
                        item = item,
                        checked = isChecked,
                        onToggle = {
                            selected = if (isChecked) selected - item.id else selected + item.id
                        },
                        onOpen = { openItem(item) }
                    )
                }
            }

            Button(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = nBrand100,
                    contentColor = nWhite100,
                    disabledContainerColor = nBlack400,
                    disabledContentColor = nBlack300
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = confirmLabel(selected.size),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isOpening) {
            BackupLoadingDialog(message = stringResource(Res.string.opening_file))
        }

        viewingFile?.let { vf ->
            IncomingFileViewer(file = vf, onClose = { viewingFile = null })
        }
    }
}

@Composable
internal fun BackupLoadingDialog(message: String) {
    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .background(nBlack400, RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = nBrand100, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = message, fontSize = 14.sp, color = nWhite100)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PreviewFileRow(
    item: BackupPreviewItem,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val mimeLabel = remember(item.mimeType) { getMimeTypeLabel(item.mimeType) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = nBlack400,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onOpen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(nBlack100, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileTypeIcon(mimeLabel),
                    contentDescription = null,
                    tint = nBrand100,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = nWhite100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.category?.takeIf { it.isNotBlank() } ?: mimeLabel,
                    fontSize = 12.sp,
                    color = nBlack300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (checked) nBrand100 else nBlack300,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
