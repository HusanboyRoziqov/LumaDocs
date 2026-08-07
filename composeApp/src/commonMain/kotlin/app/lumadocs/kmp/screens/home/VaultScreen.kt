package app.lumadocs.kmp.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.icons.LumaIcons
import app.lumadocs.kmp.platform.blobPath
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.LocalLumaColors
import app.lumadocs.kmp.theme.LumaDisplay
import app.lumadocs.kmp.theme.LumaMono
import app.lumadocs.kmp.theme.LumaUi
import app.lumadocs.kmp.ui.AllChip
import app.lumadocs.kmp.ui.CategoryChip
import app.lumadocs.kmp.ui.DocCategory
import app.lumadocs.kmp.ui.DocFileThumb
import app.lumadocs.kmp.ui.DocFolderThumb
import app.lumadocs.kmp.ui.InitialAvatar
import app.lumadocs.kmp.ui.SectionLabel
import app.lumadocs.kmp.ui.ThumbSize
import app.lumadocs.kmp.ui.categoryOf
import app.lumadocs.kmp.ui.expiryDaysOf
import app.lumadocs.kmp.ui.formatExpiry
import app.lumadocs.kmp.ui.formatSizeLocalized
import app.lumadocs.kmp.ui.formatExpiryLocalized
import app.lumadocs.kmp.ui.localizedLabel
import app.lumadocs.kmp.ui.sizedThumb
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.all_documents
import lumadocs.composeapp.generated.resources.badge_pending
import lumadocs.composeapp.generated.resources.brand_luma_docs
import lumadocs.composeapp.generated.resources.categories
import lumadocs.composeapp.generated.resources.days_short
import lumadocs.composeapp.generated.resources.documents_to_upload
import lumadocs.composeapp.generated.resources.expiring_soon
import lumadocs.composeapp.generated.resources.folder_items
import lumadocs.composeapp.generated.resources.greeting_afternoon
import lumadocs.composeapp.generated.resources.greeting_evening
import lumadocs.composeapp.generated.resources.greeting_morning
import lumadocs.composeapp.generated.resources.items_count
import lumadocs.composeapp.generated.resources.pending_size
import lumadocs.composeapp.generated.resources.pending_upload_size
import lumadocs.composeapp.generated.resources.see_all
import lumadocs.composeapp.generated.resources.tap_to_upload
import lumadocs.composeapp.generated.resources.vault_empty
import lumadocs.composeapp.generated.resources.vault_expires_suffix
import lumadocs.composeapp.generated.resources.vault_loading
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import app.lumadocs.kmp.data.PendingUpload
import app.lumadocs.kmp.utils.decodeImageBitmap
import app.lumadocs.kmp.viewmodels.DocumentsViewModel
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import okio.Path.Companion.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_SIZE = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultScreen(
    vm: DocumentsViewModel,
    user: FirebaseUser?,
    onOpenDoc: (DriveFile) -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLumaColors.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val files = state.files ?: emptyList()
    val pending by vm.pendingUploads.collectAsStateWithLifecycle(initialValue = emptyList())
    val isUploadingPending by vm.isUploadingPending.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf<DocCategory?>(null) } // null = All
    var gridView by remember { mutableStateOf(true) }

    // Derived lists are memoized: re-entering the screen (back-nav) recomposes without
    // re-categorizing / re-parsing expiry dates for the whole vault.
    val filtered = remember(files, filter) {
        if (filter == null) files else files.filter { categoryOf(it) == filter }
    }
    val expiring = remember(files) {
        files.mapNotNull { f -> expiryDaysOf(f)?.let { f to it } }
            .filter { it.second in 0..30 }
            .sortedBy { it.second }
    }
    val catCounts = remember(files) {
        DocCategory.entries.associateWith { cat -> files.count { categoryOf(it) == cat } }
    }
    val totalSize = remember(files) { files.sumOf { it.size ?: 0L } }

    val name = user?.userName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
    val greeting = greetingForNow()

    val platformContext = LocalPlatformContext.current
    LaunchedEffect(filtered, state.folderContents) {
        val loader = SingletonImageLoader.get(platformContext)
        filtered.asSequence()
            .filter {
                it.mimeType.startsWith("image/") && !it.thumbnailLink.isNullOrBlank() &&
                    (it.parentId == null || it.parentId !in state.folderContents)
            }
            .take(12)
            .forEach { f ->
                loader.enqueue(
                    ImageRequest.Builder(platformContext)
                        .data(sizedThumb(f.thumbnailLink, 400))
                        .memoryCacheKey(f.id)
                        .diskCacheKey(f.id)
                        .build()
                )
            }
    }

    val gridState = rememberLazyGridState()

    var visibleCount by rememberSaveable(filter) { mutableStateOf(PAGE_SIZE) }
    var loadingMore by remember { mutableStateOf(false) }
    val visible = filtered.take(visibleCount)
    LaunchedEffect(gridState, filtered) {
        snapshotFlow {
            val info = gridState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (lastIndex, total) ->
            if (!loadingMore && visibleCount < filtered.size && lastIndex >= total - 4) {
                loadingMore = true
                delay(400) // show the loader briefly while the next page "loads"
                visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(filtered.size)
                loadingMore = false
            }
        }
    }

    Box(modifier.fillMaxSize().background(c.bg)) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(if (gridView) 2 else 1),
            horizontalArrangement = Arrangement.spacedBy(if (gridView) 18.dp else 0.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(Res.string.brand_luma_docs), fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.6.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(if (name != null) "$greeting," else "$greeting.", fontFamily = LumaDisplay, fontSize = 34.sp, lineHeight = 34.sp, letterSpacing = (-1).sp, color = c.text)
                        if (name != null) {
                            Text(name.replaceFirstChar { it.uppercase() } + ".", fontFamily = LumaDisplay, fontStyle = FontStyle.Italic, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1).sp, color = c.text)
                        }
                    }
                    InitialAvatar(letter = name ?: "L", sizeDp = 44.dp, photoUrl = user?.userPhotoUrl)
                }
            }

            // Pending-uploads banner (offline queue) — tap to push to Drive.
            if (pending.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val pendingBytes = pending.sumOf { it.sizeBytes }
                    Row(
                        Modifier
                            .fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(c.warn.copy(alpha = 0.14f), c.warn.copy(alpha = 0.03f))))
                            .border(1.dp, c.warn.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                            .clickable(enabled = !isUploadingPending) { vm.uploadPending() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x4D000000)), contentAlignment = Alignment.Center) {
                            Icon(LumaIcons.Upload, null, tint = c.warn, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(pluralStringResource(Res.plurals.documents_to_upload, pending.size, pending.size), fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.text)
                            Text(stringResource(Res.string.tap_to_upload, formatSizeLocalized(pendingBytes)), fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        if (isUploadingPending) CircularProgressIndicator(color = c.warn, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Icon(LumaIcons.Forward, null, tint = c.warn, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expiring soon rail
            if (expiring.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionLabel(stringResource(Res.string.expiring_soon), right = {
                            Text(stringResource(Res.string.see_all), color = c.accent, fontFamily = LumaUi, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onOpenReminders))
                        })
                        Spacer(Modifier.height(12.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(expiring) { (f, days) ->
                                Row(
                                    Modifier
                                        .width(190.dp).clip(RoundedCornerShape(18.dp))
                                        .background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                                        .clickable { onOpenDoc(f) }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    DocFileThumb(file = f, size = ThumbSize.SM)
                                    Column(Modifier.weight(1f)) {
                                        Text(f.name, fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(stringResource(Res.string.days_short, days) + " · " + formatExpiryLocalized(f.expiryDate), fontFamily = LumaMono, fontSize = 10.5.sp, color = if (days <= 7) c.err else c.warn, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }
            }

            // Category chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionLabel(stringResource(Res.string.categories))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { AllChip(active = filter == null, count = files.size, onClick = { filter = null }) }
                        items(DocCategory.entries.filter { (catCounts[it] ?: 0) > 0 }) { cat ->
                            CategoryChip(category = cat, active = filter == cat, count = catCounts[cat], onClick = { filter = cat })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Documents header + view toggle
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${(filter?.localizedLabel() ?: stringResource(Res.string.all_documents)).uppercase()} · ${filtered.size}",
                        fontFamily = LumaMono, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = c.textMute, letterSpacing = 1.4.sp,
                    )
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x0AFFFFFF)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(true, false).forEach { grid ->
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(if (gridView == grid) Color(0x14FFFFFF) else Color.Transparent).clickable { gridView = grid }.padding(horizontal = 8.dp, vertical = 5.dp),
                            ) { Icon(if (grid) LumaIcons.Grid else LumaIcons.ListView, null, tint = if (gridView == grid) c.text else c.textMute, modifier = Modifier.size(15.dp)) }
                        }
                    }
                }
            }

            // Pending (offline) uploads — shown first, straight from local cache.
            items(items = pending, key = { "pending_${it.id}" }, span = { GridItemSpan(if (gridView) 1 else maxLineSpan) }) { p ->
                PendingCard(vm = vm, item = p, gridView = gridView)
            }

            // Documents
            if (state.isLoading && files.isEmpty() && pending.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { CenteredHint(stringResource(Res.string.vault_loading)) }
            } else if (filtered.isEmpty() && pending.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { CenteredHint(stringResource(Res.string.vault_empty)) }
            } else if (filtered.isNotEmpty()) {
                items(
                    items = visible,
                    key = { it.id },
                    span = { GridItemSpan(if (gridView) 1 else maxLineSpan) },
                ) { f ->
                    // Folder documents show a folder icon instead of loading their photos — much
                    // faster to scroll. `folderContents` keys are the user sub-folder ids.
                    val folderId = f.parentId?.takeIf { it in state.folderContents }
                    val folderCount = folderId?.let { state.folderContents[it]?.size ?: 0 }
                    val displayName = folderId?.let { state.folderNames[it] } ?: f.name
                    if (gridView) {
                        Column(Modifier.padding(start = 20.dp, bottom = 18.dp).clickable { onOpenDoc(f) }) {
                            if (folderId != null) DocFolderThumb(count = folderCount ?: 0, size = ThumbSize.MD, fillWidth = true)
                            else DocFileThumb(file = f, size = ThumbSize.MD, fillWidth = true)
                            Text(displayName, fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                            Text(
                                if (folderId != null) stringResource(Res.string.folder_items, pluralStringResource(Res.plurals.items_count, folderCount ?: 0, folderCount ?: 0))
                                else "${categoryOf(f).localizedLabel()} · ${formatSizeLocalized(f.size)}",
                                fontFamily = LumaMono, fontSize = 10.5.sp, color = c.textMute, letterSpacing = 0.2.sp, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                                .clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.hairline, RoundedCornerShape(14.dp))
                                .clickable { onOpenDoc(f) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (folderId != null) DocFolderThumb(count = folderCount ?: 0, size = ThumbSize.SM)
                            else DocFileThumb(file = f, size = ThumbSize.SM)
                            Column(Modifier.weight(1f)) {
                                Text(displayName, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (folderId != null) {
                                        stringResource(Res.string.folder_items, pluralStringResource(Res.plurals.items_count, folderCount ?: 0, folderCount ?: 0))
                                    } else {
                                        val categoryLabel = categoryOf(f).localizedLabel()
                                        val expiryText = f.expiryDate?.let { stringResource(Res.string.vault_expires_suffix, formatExpiryLocalized(it)) }.orEmpty()
                                        "$categoryLabel · ${formatSizeLocalized(f.size)}$expiryText"
                                    },
                                    fontFamily = LumaMono, fontSize = 11.sp, color = c.textMute, letterSpacing = 0.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            Icon(LumaIcons.Chevron, null, tint = c.textMute, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                // Loader shown while the next page loads.
                if (visibleCount < filtered.size) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

/** A queued (offline) document card — image loaded by Coil straight from its cached file. */
@Composable
private fun PendingCard(vm: DocumentsViewModel, item: PendingUpload, gridView: Boolean) {
    val c = LocalLumaColors.current
    val isImage = item.mimeType.startsWith("image/")
    val ctx = LocalPlatformContext.current
    // Coil decodes from the blob file, downsampled to the cell and memory-cached by id —
    // no full-size decode in composition, no flicker on re-scroll.
    val request = remember(item.id) {
        val path = if (isImage) blobPath(item.id) else null
        path?.let {
            ImageRequest.Builder(ctx)
                .data(it.toPath())
                .memoryCacheKey(item.id)
                .placeholderMemoryCacheKey(item.id)
                .build()
        }
    }
    if (gridView) {
        Column(Modifier.padding(start = 20.dp, bottom = 18.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(ThumbSize.MD.w.value / ThumbSize.MD.h.value).clip(RoundedCornerShape(10.dp)).background(c.bg2)) {
                if (request != null) AsyncImage(model = request, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else Icon(LumaIcons.Upload, null, tint = c.warn, modifier = Modifier.align(Alignment.Center).size(40.dp))
                PendingBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
            }
            Text(item.name, fontFamily = LumaUi, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            Text(stringResource(Res.string.pending_size, formatSizeLocalized(item.sizeBytes)), fontFamily = LumaMono, fontSize = 10.5.sp, color = c.warn, letterSpacing = 0.2.sp, modifier = Modifier.padding(top = 2.dp))
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp)).background(c.bg2).border(1.dp, c.warn.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(ThumbSize.SM.w, ThumbSize.SM.h).clip(RoundedCornerShape(10.dp)).background(c.bg3)) {
                if (request != null) AsyncImage(model = request, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else Icon(LumaIcons.Upload, null, tint = c.warn, modifier = Modifier.align(Alignment.Center).size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, fontFamily = LumaUi, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(Res.string.pending_upload_size, formatSizeLocalized(item.sizeBytes)), fontFamily = LumaMono, fontSize = 11.sp, color = c.warn, letterSpacing = 0.3.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun PendingBadge(modifier: Modifier = Modifier) {
    val c = LocalLumaColors.current
    Box(modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xE6000000)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(stringResource(Res.string.badge_pending), fontFamily = LumaMono, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = c.warn, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun CenteredHint(text: String) {
    val c = LocalLumaColors.current
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = c.textMute, fontFamily = LumaUi, fontSize = 14.sp)
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun greetingForNow(): String {
    val hour = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return stringResource(
        when (hour) {
            in 5..11 -> Res.string.greeting_morning
            in 12..17 -> Res.string.greeting_afternoon
            else -> Res.string.greeting_evening
        }
    )
}
