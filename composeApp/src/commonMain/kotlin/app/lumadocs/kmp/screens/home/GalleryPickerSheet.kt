package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumadocs.kmp.platform.GalleryImage
import app.lumadocs.kmp.platform.isPartialGalleryAccess
import app.lumadocs.kmp.platform.loadGalleryImages
import app.lumadocs.kmp.platform.rememberReselectPhotos
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import coil3.compose.AsyncImage
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.add_selected_count
import lumadocs.composeapp.generated.resources.allow_photo_access
import lumadocs.composeapp.generated.resources.file_label
import lumadocs.composeapp.generated.resources.gallery_title
import lumadocs.composeapp.generated.resources.grant_access
import lumadocs.composeapp.generated.resources.no_photos_found
import lumadocs.composeapp.generated.resources.select_more_photos
import org.jetbrains.compose.resources.stringResource

/**
 * Telegram-style attachment sheet: a grid of gallery photos with multi-select, plus a
 * "File" action for picking any file. Confirming returns the selected image URIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryPickerSheet(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickFiles: () -> Unit,
    onCameraClick: () -> Unit,
    onConfirmImages: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<List<String>>(emptyList()) }
    var partialAccess by remember { mutableStateOf(isPartialGalleryAccess()) }
    // Bumped after the user re-selects photos, to reload the (now larger) subset.
    var reloadKey by remember { mutableStateOf(0) }

    val reselectPhotos = rememberReselectPhotos { reloadKey++ }

    LaunchedEffect(hasPermission, reloadKey) {
        if (hasPermission) {
            loading = true
            images = loadGalleryImages()
            partialAccess = isPartialGalleryAccess()
            loading = false
        } else {
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = nBlack100,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.gallery_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = nWhite100,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    onClick = onPickFiles,
                    shape = RoundedCornerShape(50),
                    color = nBlack400
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = nBrand100,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(Res.string.file_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = nBrand100
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !hasPermission -> PermissionPrompt(onRequestPermission)

                    loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = nBrand100, modifier = Modifier.size(36.dp))
                    }

                    images.isEmpty() && !partialAccess -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.no_photos_found),
                            color = nBlack300,
                            fontSize = 14.sp
                        )
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item(key = "camera") { CameraCell(onClick = onCameraClick) }
                        // Partial ("Selected photos") access → let the user add more photos.
                        if (partialAccess) {
                            item(key = "manage") { SelectMoreCell(onClick = reselectPhotos) }
                        }
                        items(images, key = { it.id }) { img ->
                            val order = selected.indexOf(img.uri)
                            GalleryCell(
                                uri = img.uri,
                                selectionNumber = if (order >= 0) order + 1 else null,
                                onClick = {
                                    selected =
                                        if (order >= 0) selected - img.uri else selected + img.uri
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onConfirmImages(selected) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                    text = stringResource(Res.string.add_selected_count, selected.size),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.allow_photo_access),
            fontSize = 14.sp,
            color = nBlack300,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            color = nBrand100
        ) {
            Text(
                text = stringResource(Res.string.grant_access),
                color = nWhite100,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun SelectMoreCell(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(nBrand100.copy(alpha = 0.12f))
            .border(1.dp, nBrand100.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.AddPhotoAlternate,
            contentDescription = null,
            tint = nBrand100,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.select_more_photos),
            color = nBrand100,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(nBlack400)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = "Camera",
            tint = nBrand100,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun GalleryCell(uri: String, selectionNumber: Int?, onClick: () -> Unit) {
    val isSelected = selectionNumber != null
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(nBlack400)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(nBrand100.copy(alpha = 0.28f)))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) nBrand100 else Color.Black.copy(alpha = 0.35f))
                .border(1.5.dp, nWhite100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selectionNumber != null) {
                Text(
                    text = "$selectionNumber",
                    color = nWhite100,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
