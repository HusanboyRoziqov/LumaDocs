package app.lumadocs.kmp.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.lumadocs.kmp.BannerSettingsViewModel
import app.lumadocs.kmp.DocumentsViewMode
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data_store.rememberDataStore
import app.lumadocs.kmp.icons.GoogleDriveIcon
import app.lumadocs.kmp.paging.SnapshotPagingSource
import app.lumadocs.kmp.platform.isOnline
import app.lumadocs.kmp.services.DriveFile
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBlack300
import app.lumadocs.kmp.theme.nBlack400
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.theme.nShimmer
import app.lumadocs.kmp.theme.nShimmerHi
import app.lumadocs.kmp.theme.nWhite100
import app.lumadocs.kmp.viewmodels.DocumentsViewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.encrypted_image
import lumadocs.composeapp.generated.resources.files
import lumadocs.composeapp.generated.resources.no_internet
import lumadocs.composeapp.generated.resources.not_found_files
import lumadocs.composeapp.generated.resources.refreshing
import lumadocs.composeapp.generated.resources.search_files
import lumadocs.composeapp.generated.resources.sort_alpha_asc
import lumadocs.composeapp.generated.resources.sort_alpha_desc
import lumadocs.composeapp.generated.resources.sort_by
import lumadocs.composeapp.generated.resources.sort_date_asc_sub
import lumadocs.composeapp.generated.resources.sort_date_desc_sub
import lumadocs.composeapp.generated.resources.sort_name_az
import lumadocs.composeapp.generated.resources.sort_name_za
import lumadocs.composeapp.generated.resources.sort_newest_first
import lumadocs.composeapp.generated.resources.sort_oldest_first
import lumadocs.composeapp.generated.resources.unknown_error
import lumadocs.composeapp.generated.resources.upload_first_image
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

enum class SortOrder {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC
}

@Composable
private fun RefreshBanner(
    offline: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRefresh: () -> Unit,
) {
    Surface(
        color = nBlack400,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = bottomPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (offline) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.no_internet),
                    color = nWhite100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            } else {
                CircularProgressIndicator(
                    color = nBrand100,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.refreshing),
                    color = nWhite100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = nBrand100
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentsScreen(
    modifier: Modifier = Modifier,
    user: FirebaseUser?,
    paddingValues: PaddingValues,
    onNavigateToDetail: (DriveFile) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val viewModel = koinViewModel<DocumentsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val dataStore = rememberDataStore()
    val bannerViewModel = viewModel { BannerSettingsViewModel(dataStore) }
    val bannerEnabled by bannerViewModel.bannerEnabled.collectAsStateWithLifecycle()
    val viewModeName by bannerViewModel.viewMode.collectAsStateWithLifecycle()
    val viewMode = remember(viewModeName) {
        runCatching { DocumentsViewMode.valueOf(viewModeName) }.getOrDefault(DocumentsViewMode.LIST)
    }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var showSortSheet by remember { mutableStateOf(false) }

    var offline by remember { mutableStateOf(!isOnline()) }
    LaunchedEffect(Unit) {
        while (true) {
            offline = !isOnline()
            kotlinx.coroutines.delay(3000)
        }
    }

    val filteredFiles = remember(searchQuery, sortOrder, uiState.files) {
        var result = uiState.files
        if (searchQuery.isNotEmpty()) {
            result = result?.filter { f ->
                f.name.contains(searchQuery, ignoreCase = true) ||
                        (f.parentId != null &&
                                uiState.folderNames[f.parentId]?.contains(
                                    searchQuery,
                                    ignoreCase = true
                                ) == true)
            }
        }
        result = when (sortOrder) {
            SortOrder.DATE_DESC -> result?.sortedByDescending { it.createdTime }
            SortOrder.DATE_ASC -> result?.sortedBy { it.createdTime }
            SortOrder.NAME_ASC -> result?.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> result?.sortedByDescending { it.name.lowercase() }
        }
        result
    }

    val pagingFlow = remember(filteredFiles) {
        Pager(
            config = PagingConfig(
                pageSize = SnapshotPagingSource.DEFAULT_PAGE_SIZE,
                initialLoadSize = SnapshotPagingSource.DEFAULT_PAGE_SIZE,
                prefetchDistance = 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { SnapshotPagingSource(filteredFiles ?: emptyList()) }
        ).flow
    }
    val pagingItems = pagingFlow.collectAsLazyPagingItems()

    Box(modifier = modifier.fillMaxSize().background(nBlack100)) {
        when {
            uiState.isLoading && uiState.files == null -> {
                DocumentsShimmerLoading(paddingValues = paddingValues, user = user)
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = nBlack300,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: stringResource(Res.string.unknown_error),
                            fontSize = 15.sp,
                            color = nBlack300,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                val gridState = rememberLazyGridState()
                var stickyActivated by remember { mutableStateOf(false) }
                val showStickyHeader by remember {
                    derivedStateOf {
                        stickyActivated &&
                                (gridState.firstVisibleItemIndex > 0 ||
                                        gridState.firstVisibleItemScrollOffset > 0)
                    }
                }
                LaunchedEffect(gridState) {
                    var prevIndex = 0
                    var prevOffset = 0
                    val fileCardsStartIndex = 5
                    snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
                        .distinctUntilChanged()
                        .collect { (index, offset) ->
                            val atTop = index == 0 && offset == 0
                            val inFileCardsZone = index >= fileCardsStartIndex
                            val scrollingUp =
                                index < prevIndex || (index == prevIndex && offset < prevOffset)
                            when {
                                atTop -> stickyActivated = false
                                inFileCardsZone && scrollingUp -> stickyActivated = true
                                !scrollingUp -> stickyActivated = false
                            }
                            prevIndex = index
                            prevOffset = offset
                        }
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isGrid = viewMode == DocumentsViewMode.GRID
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(if (isGrid) 2 else 1),
                        horizontalArrangement = Arrangement.spacedBy(if (isGrid) 12.dp else 0.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 46.dp,
                            bottom = paddingValues.calculateBottomPadding() + 42.dp
                        )
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HeaderSection(
                                user = user,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onProfileClick = onNavigateToSettings
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.height(20.dp))
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StatsRow(
                                totalFiles = uiState.totalFiles,
                                sortOrder = sortOrder,
                                onSortClick = { showSortSheet = true },
                                viewMode = viewMode,
                                onToggleViewMode = {
                                    bannerViewModel.setViewMode(
                                        if (viewMode == DocumentsViewMode.GRID) DocumentsViewMode.LIST
                                        else DocumentsViewMode.GRID
                                    )
                                }
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.height(12.dp))
                        }

                        if (filteredFiles?.isEmpty() == true) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyState()
                            }
                        } else {
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey { it.id },
                                contentType = pagingItems.itemContentType { getMimeTypeLabel(it.mimeType) }
                            ) { index ->
                                val file = pagingItems[index]
                                if (file != null) {
                                    val folderFiles =
                                        file.parentId?.let { uiState.folderContents[it] }
                                    val onCardClick: (DriveFile) -> Unit = {
                                        if (folderFiles != null) selectedFolder = file.parentId
                                        else onNavigateToDetail(file)
                                    }
                                    if (isGrid) {
                                        FileGridCard(
                                            modifier = Modifier.padding(bottom = 12.dp),
                                            file = file,
                                            folderCount = folderFiles?.size,
                                            onClick = onCardClick
                                        )
                                    } else {
                                        FileCard(
                                            modifier = Modifier.padding(bottom = 10.dp),
                                            file = file,
                                            folderCount = folderFiles?.size,
                                            onClick = onCardClick
                                        )
                                    }
                                }
                            }

                            if (pagingItems.loadState.append is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = nBrand100,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = showStickyHeader,
                        enter = slideInVertically { -it },
                        exit = slideOutVertically(animationSpec = tween(0)) { -it }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(nBlack100)
                                .padding(start = 20.dp, end = 20.dp, top = 46.dp, bottom = 12.dp)
                        ) {
                            HeaderSection(
                                user = user,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onProfileClick = onNavigateToSettings
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = bannerEnabled && (offline || uiState.isRefreshing),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            RefreshBanner(
                offline = offline,
                bottomPadding = paddingValues.calculateBottomPadding() + 100.dp,
                onRefresh = { viewModel.refresh() }
            )
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            current = sortOrder,
            onSelect = { sortOrder = it; showSortSheet = false },
            onDismiss = { showSortSheet = false }
        )
    }

    // Single-file detail now opens as a full navigation screen (Route.DocumentDetail),
    // triggered via onNavigateToDetail. Folders still use the bottom sheet below.
    if (selectedFolder != null) {
        val folderId = selectedFolder!!
        val folderFiles = uiState.folderContents[folderId] ?: emptyList()
        val folderName = uiState.folderNames[folderId] ?: ""
        ModalBottomSheet(
            onDismissRequest = { selectedFolder = null },
            sheetState = bottomSheetState,
            containerColor = nBlack100,
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = { BottomSheetDefaults.DragHandle(color = nBlack400) }
        ) {
            Box(modifier = Modifier.navigationBarsPadding()) {
                FolderDetailRoute(
                    files = folderFiles,
                    folderName = folderName,
                    onClose = { selectedFolder = null },
                    onDeleted = {
                        selectedFolder = null
                        viewModel.refresh()
                    }
                )
            }
        }
    }
}

@Composable
fun DocumentsShimmerLoading(
    paddingValues: PaddingValues,
    user: FirebaseUser?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 46.dp,
                    bottom = paddingValues.calculateBottomPadding() + 42.dp
                )
            )
    ) {
        HeaderSection(user = user)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmerEffect()
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.width(100.dp).height(16.dp).clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        )
    )

    val base = nShimmer
    val highlight = nShimmerHi
    background(
        brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
private fun HeaderSection(
    user: FirebaseUser? = null,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val windowInsets = androidx.compose.foundation.layout.WindowInsets.ime
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(Unit) {
        snapshotFlow { windowInsets.getBottom(density) }
            .distinctUntilChanged()
            .collect { imeBottom -> if (imeBottom == 0) focusManager.clearFocus() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Luma Docs",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = nWhite100
                )
                val subtitleName = user?.userName?.takeIf { it.isNotBlank() && it != "null" }
                if (subtitleName != null) {
                    Text(text = subtitleName, fontSize = 13.sp, color = nBlack300)
                }
            }

            val photoUrlString = user?.userPhotoUrl
            var imageLoadFailed by remember(photoUrlString) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(nBlack400)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrlString.isNullOrEmpty() && !imageLoadFailed) {
                    AsyncImage(
                        model = photoUrlString,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true }
                    )
                } else {
                    val userName = user?.userName?.takeIf { it.isNotBlank() && it != "null" }
                    if (userName == null) {
                        // Not signed in — show the Google Drive logo (same as the Settings screen).
                        Icon(
                            imageVector = GoogleDriveIcon,
                            contentDescription = "Google Drive",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    } else {
                        val initials = remember(userName) {
                            userName
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                                .take(2)
                                .ifEmpty { "U" }
                        }
                        Text(
                            text = initials,
                            color = nWhite100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().height(52.dp).focusRequester(focusRequester),
            placeholder = {
                Text(stringResource(Res.string.search_files), color = nBlack300, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = nBlack300,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            textStyle = TextStyle(color = nWhite100, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = nBrand100,
                unfocusedBorderColor = nBlack400,
                focusedContainerColor = nBlack400,
                unfocusedContainerColor = nBlack400,
                cursorColor = nBrand100
            )
        )
    }
}

@Composable
private fun StatsRow(
    totalFiles: Int = 0,
    sortOrder: SortOrder = SortOrder.DATE_DESC,
    onSortClick: () -> Unit = {},
    viewMode: DocumentsViewMode = DocumentsViewMode.LIST,
    onToggleViewMode: () -> Unit = {},
) {
    val sortLabel = when (sortOrder) {
        SortOrder.DATE_DESC -> stringResource(Res.string.sort_newest_first)
        SortOrder.DATE_ASC -> stringResource(Res.string.sort_oldest_first)
        SortOrder.NAME_ASC -> stringResource(Res.string.sort_name_az)
        SortOrder.NAME_DESC -> stringResource(Res.string.sort_name_za)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$totalFiles ${stringResource(Res.string.files)}",
            color = nWhite100,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onToggleViewMode,
                color = nBlack400,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (viewMode == DocumentsViewMode.GRID) {
                        Icons.AutoMirrored.Filled.ViewList
                    } else {
                        Icons.Filled.GridView
                    },
                    contentDescription = "Toggle view",
                    tint = nBrand100,
                    modifier = Modifier.padding(7.dp).size(16.dp)
                )
            }

            Surface(
                onClick = onSortClick,
                color = nBlack400,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = nBrand100,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = sortLabel,
                        color = nBrand100,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).background(nBlack400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = nBlack300,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.not_found_files),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = nWhite100
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.upload_first_image),
                fontSize = 13.sp,
                color = nBlack300,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    data class SortOption(val order: SortOrder, val label: String, val sub: String)

    val options = listOf(
        SortOption(
            SortOrder.DATE_DESC,
            stringResource(Res.string.sort_newest_first),
            stringResource(Res.string.sort_date_desc_sub)
        ),
        SortOption(
            SortOrder.DATE_ASC,
            stringResource(Res.string.sort_oldest_first),
            stringResource(Res.string.sort_date_asc_sub)
        ),
        SortOption(
            SortOrder.NAME_ASC,
            stringResource(Res.string.sort_name_az),
            stringResource(Res.string.sort_alpha_asc)
        ),
        SortOption(
            SortOrder.NAME_DESC,
            stringResource(Res.string.sort_name_za),
            stringResource(Res.string.sort_alpha_desc)
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = nBlack400,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle(color = nBlack300) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.sort_by),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = nWhite100,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            options.forEach { opt ->
                val isSelected = current == opt.order
                Surface(
                    onClick = { onSelect(opt.order) },
                    color = if (isSelected) nBrand100.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = opt.label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) nBrand100 else nWhite100
                            )
                            Text(
                                text = opt.sub,
                                fontSize = 12.sp,
                                color = nBlack300
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = nBrand100,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    modifier: Modifier = Modifier,
    file: DriveFile,
    folderCount: Int? = null,
    onClick: (DriveFile) -> Unit = {},
) {
    val mimeTypeLabel = remember(file.mimeType) { getMimeTypeLabel(file.mimeType) }
    val isImage = mimeTypeLabel == "Image"
    var isImageLoading by remember(file.id) { mutableStateOf(isImage && file.thumbnailLink != null) }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick(file) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = nBlack400),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(nBlack100)
                    .then(if (isImageLoading) Modifier.shimmerEffect() else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    if (file.thumbnailLink == null) {
                        Image(
                            painter = painterResource(Res.drawable.encrypted_image),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = file.thumbnailLink,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onState = { isImageLoading = it is AsyncImagePainter.State.Loading }
                        )
                    }
                } else {
                    Icon(
                        imageVector = fileTypeIcon(mimeTypeLabel),
                        contentDescription = null,
                        tint = nBrand100,
                        modifier = Modifier.size(40.dp)
                    )
                }

                if (file.encrypted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .background(nBrand100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                if (folderCount != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(nBrand100, RoundedCornerShape(50))
                            .padding(horizontal = 5.dp, vertical = (2.5).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "$folderCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    color = nWhite100,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                if (file.expiryDate.isNullOrEmpty()) {
                    Text(
                        text = formatDisplayDate(file.createdTime) ?: "",
                        color = nBlack300,
                        fontSize = 12.sp
                    )
                }

                if (!file.description.isNullOrEmpty()) {
                    Text(
                        text = file.description!!,
                        color = nBlack300,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (!file.expiryDate.isNullOrEmpty()) {
                    val urgency = remember(file.expiryDate) { expiryUrgency(file.expiryDate!!) }
                    val badgeColor = when (urgency) {
                        ExpiryUrgency.EXPIRED -> Color(0xFFFF3B30)
                        ExpiryUrgency.TODAY -> Color(0xFFFF3B30)
                        ExpiryUrgency.SOON -> Color(0xFFFF9500)
                        ExpiryUrgency.UPCOMING -> Color(0xFF34C759)
                    }
                    FileBadge(
                        label = file.expiryDate!!,
                        containerColor = badgeColor.copy(alpha = 0.15f),
                        textColor = badgeColor,
                        icon = {
                            Icon(
                                Icons.Filled.Timer,
                                null,
                                tint = badgeColor,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileGridCard(
    modifier: Modifier = Modifier,
    file: DriveFile,
    folderCount: Int? = null,
    onClick: (DriveFile) -> Unit = {},
) {
    val mimeTypeLabel = remember(file.mimeType) { getMimeTypeLabel(file.mimeType) }
    val isImage = mimeTypeLabel == "Image"
    var isImageLoading by remember(file.id) { mutableStateOf(isImage && file.thumbnailLink != null) }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick(file) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = nBlack400),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(nBlack100)
                    .then(if (isImageLoading) Modifier.shimmerEffect() else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    if (file.thumbnailLink == null) {
                        Image(
                            painter = painterResource(Res.drawable.encrypted_image),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        AsyncImage(
                            model = file.thumbnailLink,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onState = { isImageLoading = it is AsyncImagePainter.State.Loading }
                        )
                    }
                } else {
                    Icon(
                        imageVector = fileTypeIcon(mimeTypeLabel),
                        contentDescription = null,
                        tint = nBrand100,
                        modifier = Modifier.size(52.dp)
                    )
                }

                if (file.encrypted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .background(nBrand100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (folderCount != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(nBrand100, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "$folderCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = file.name,
                fontWeight = FontWeight.SemiBold,
                color = nWhite100,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            if (!file.expiryDate.isNullOrEmpty()) {
                val urgency = remember(file.expiryDate) { expiryUrgency(file.expiryDate!!) }
                val badgeColor = when (urgency) {
                    ExpiryUrgency.EXPIRED -> Color(0xFFFF3B30)
                    ExpiryUrgency.TODAY -> Color(0xFFFF3B30)
                    ExpiryUrgency.SOON -> Color(0xFFFF9500)
                    ExpiryUrgency.UPCOMING -> Color(0xFF34C759)
                }
                FileBadge(
                    label = file.expiryDate!!,
                    containerColor = badgeColor.copy(alpha = 0.15f),
                    textColor = badgeColor,
                    icon = {
                        Icon(
                            Icons.Filled.Timer,
                            null,
                            tint = badgeColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                )
            } else {
                Text(
                    text = formatDisplayDate(file.createdTime) ?: "",
                    color = nBlack300,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FileBadge(
    label: String,
    containerColor: Color,
    textColor: Color,
    icon: @Composable () -> Unit,
) {
    Surface(color = containerColor, shape = RoundedCornerShape(6.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            icon()
            Text(text = label, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Normalizes a raw Drive date string to a clean `yyyy-MM-dd` for display. Unlike a bare
 * `take(10)`, this parses the value first, so it handles full RFC3339 timestamps (with a time
 * component), epoch-millis stored as a number string, and plain ISO dates alike — which is why
 * encrypted files (whose stored date can differ in format) previously showed a long timestamp.
 * Falls back to the first 10 characters only when the format is unrecognized.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
fun formatDisplayDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val zone = TimeZone.currentSystemDefault()
    // Epoch millis stored as a plain number.
    raw.toLongOrNull()?.let { millis ->
        return runCatching {
            kotlin.time.Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date.toString()
        }.getOrNull() ?: raw
    }
    // RFC3339 / ISO-8601 timestamp with a time (and possibly offset) component.
    runCatching { kotlin.time.Instant.parse(raw) }.getOrNull()?.let {
        return it.toLocalDateTime(zone).date.toString()
    }
    // ISO local date-time without an offset.
    runCatching { LocalDateTime.parse(raw) }.getOrNull()?.let { return it.date.toString() }
    // Already a plain date, or an unknown format.
    return runCatching { LocalDate.parse(raw).toString() }.getOrNull() ?: raw.take(10)
}

enum class ExpiryUrgency { EXPIRED, TODAY, SOON, UPCOMING }

@OptIn(kotlin.time.ExperimentalTime::class)
fun expiryUrgency(isoDate: String): ExpiryUrgency {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull()
        ?: return ExpiryUrgency.UPCOMING
    val now = Clock.System.now()
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val daysLeft = (date.toEpochDays() - today.toEpochDays()).toInt()
    return when {
        daysLeft < 0 -> ExpiryUrgency.EXPIRED
        daysLeft == 0 -> ExpiryUrgency.TODAY
        daysLeft <= 7 -> ExpiryUrgency.SOON
        else -> ExpiryUrgency.UPCOMING
    }
}

@Composable
internal expect fun openDriveFile(file: DriveFile)

internal fun getMimeTypeLabel(mimeType: String): String = when {
    mimeType.contains("image", ignoreCase = true) -> "Image"
    mimeType.contains("pdf", ignoreCase = true) -> "PDF"
    mimeType.contains("document", ignoreCase = true) -> "Document"
    mimeType.contains("word", ignoreCase = true) -> "Document"
    mimeType.contains("sheet", ignoreCase = true) -> "Spreadsheet"
    mimeType.contains("presentation", ignoreCase = true) -> "Presentation"
    else -> "File"
}

internal fun fileTypeIcon(mimeTypeLabel: String): ImageVector = when (mimeTypeLabel) {
    "Image" -> Icons.Filled.Image
    "PDF" -> Icons.Filled.PictureAsPdf
    "Document" -> Icons.Filled.Description
    "Spreadsheet" -> Icons.Filled.TableChart
    "Presentation" -> Icons.Filled.Slideshow
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}
