package app.lumadocs.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.lumadocs.kmp.data_store.LocalAppLocale
import app.lumadocs.kmp.data_store.rememberDataStore
import app.lumadocs.kmp.navigation.NavConfig
import app.lumadocs.kmp.navigation.Route
import app.lumadocs.kmp.platform.SystemBarsAppearance
import app.lumadocs.kmp.platform.addPlatformComponents
import app.lumadocs.kmp.platform.imageCacheDir
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath
import app.lumadocs.kmp.screens.IncomingBackupImporter
import app.lumadocs.kmp.screens.IncomingFileViewer
import app.lumadocs.kmp.screens.OnboardingPager
import app.lumadocs.kmp.screens.PinFlowScreen
import app.lumadocs.kmp.screens.PinMode
import app.lumadocs.kmp.screens.PinScreen
import app.lumadocs.kmp.screens.StartedScreen
import app.lumadocs.kmp.screens.home.DocumentDetailRoute
import app.lumadocs.kmp.screens.home.HomeScreen
import app.lumadocs.kmp.theme.LumaDocsTheme
import app.lumadocs.kmp.theme.nBlack100
import app.lumadocs.kmp.theme.nBrand100
import app.lumadocs.kmp.viewmodels.DocumentsViewModel
import app.lumadocs.kmp.viewmodels.StartScreenViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun LumaDocs() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { addPlatformComponents() }
            // Bigger in-memory bitmap cache so list thumbnails stay cached while scrolling
            // (prevents re-decoding/flicker of large images), plus a soft cross-fade.
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(context, 0.30).build()
            }
            // Persistent disk cache: thumbnails download once, then load from disk on every
            // later scroll instead of hitting the network — kills the reload flicker.
            .diskCache {
                DiskCache.Builder()
                    .directory((imageCacheDir() + "/coil_image_cache").toPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    val dataStore = rememberDataStore()
    val themeViewModel = viewModel { ThemeViewModel(dataStore) }
    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

    LumaDocsTheme(isDarkTheme = isDarkMode) {
        // Keep the transparent system bars' icon contrast in sync with the app theme.
        SystemBarsAppearance(darkTheme = isDarkMode)

        val viewModel = viewModel { LanguageViewModel(dataStore) }
        val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
        val startScreenViewModel: StartScreenViewModel = koinViewModel()

        val pinViewModel = viewModel { PinViewModel(dataStore) }
        val lockEnabled by pinViewModel.lockEnabled.collectAsStateWithLifecycle()
        var locked by remember { mutableStateOf(lockEnabled) }
        val lockTimeoutMs = 5 * 60 * 1000L
        var backgroundedAt by remember { mutableStateOf<Long?>(null) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP ->
                        backgroundedAt = Clock.System.now().toEpochMilliseconds()

                    Lifecycle.Event.ON_START -> {
                        val since = backgroundedAt
                        if (since != null &&
                            Clock.System.now().toEpochMilliseconds() - since > lockTimeoutMs
                        ) {
                            locked = true
                        }
                        backgroundedAt = null
                    }

                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val startDestination = if (startScreenViewModel.checkUser() == null) {
            Route.Onboarding
        } else {
            startScreenViewModel.checkCurrentUser()
            val state by startScreenViewModel.uiState.collectAsStateWithLifecycle()
            Route.Home(state.data)
        }

        val backStack = rememberNavBackStack(
            configuration = NavConfig, startDestination
        )

        Box(modifier = Modifier.fillMaxSize().background(nBlack100)) {
            CompositionLocalProvider(LocalAppLocale provides languageCode) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }) { key ->
                    NavEntry(key) {
                        when (key) {
                            is Route.Onboarding -> OnboardingPager(
                                onGetStarted = {
                                    backStack.add(Route.Started)
                                },
                                onSkipIntro = {
                                    backStack.add(Route.Home())
                                }
                            )

                            is Route.Started -> StartedScreen { user ->
                                backStack.add(Route.Home(user))
                            }

                            is Route.DocumentDetail -> {

                                val docsViewModel: DocumentsViewModel = koinViewModel()
                                val state by docsViewModel.uiState.collectAsStateWithLifecycle()
                                val file = state.files?.find { it.id == key.fileId }
                                if (file != null) {
                                    // If this file was uploaded as a folder of pages, page through them all.
                                    val folderPages = file.parentId?.let { state.folderContents[it] }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(nBlack100)
                                    ) {
                                        DocumentDetailRoute(
                                            file = file,
                                            pages = folderPages ?: listOf(file),
                                            onClose = {
                                                if (backStack.size > 1) backStack.removeAt(
                                                    backStack.size - 1
                                                )
                                            },
                                            onDeleted = {

                                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                                docsViewModel.refreshFiles()
                                            },
                                            // Stay on the info screen after saving; just refresh the
                                            // underlying list (including the expiry date) in place.
                                            onSaved = { updated ->
                                                docsViewModel.updateFileInList(updated)
                                            }
                                        )
                                    }
                                } else {
                                    LaunchedEffect(state.files) {
                                        val files = state.files
                                        if (files != null) {
                                            val found = files.find { it.id == key.fileId }
                                            if (found == null) {
                                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(nBlack100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = nBrand100)
                                    }
                                }
                            }

                            is Route.Home -> HomeScreen(
                                user = key.user,
                                onNavigateToDetail = { fileId ->
                                    backStack.add(
                                        Route.DocumentDetail(
                                            fileId
                                        )
                                    )
                                },
                                onNavigateToScan = { backStack.add(Route.Scan) }
                            )

                            is Route.Scan -> app.lumadocs.kmp.screens.home.ScanScreen(
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                                onSaved = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                            )
                        }
                    }
                }
            }

            val openFileId by OpenFileState.fileId.collectAsState()
            LaunchedEffect(openFileId) {
                val id = openFileId ?: return@LaunchedEffect
                backStack.add(app.lumadocs.kmp.navigation.Route.DocumentDetail(id))
                OpenFileState.clear()
            }

            // After a disconnect / session wipe in Settings, reset the whole
            // backstack so the user lands on the first onboarding screen.
            val sessionReset by SessionResetState.reset.collectAsState()
            LaunchedEffect(sessionReset) {
                if (!sessionReset) return@LaunchedEffect
                locked = false
                backStack.clear()
                backStack.add(Route.Onboarding)
                SessionResetState.clear()
            }

            val incomingFile by IncomingFileState.file.collectAsState()
            val incoming = incomingFile
            if (incoming != null) {
                IncomingFileViewer(
                    file = incoming,
                    onClose = { IncomingFileState.clear() }
                )
            }

            IncomingBackupImporter()

            if (lockEnabled && locked) {
                PinScreen(
                    mode = PinMode.UNLOCK,
                    onUnlock = { locked = false },
                )
            }

            // Full-screen PIN set/change/remove flow (covers the bottom nav).
            val pinFlow by PinFlowState.request.collectAsState()
            pinFlow?.let { request ->
                PinFlowScreen(
                    request = request,
                    onDone = {
                        PinFlowState.clear()
                        // Identity was just confirmed via Google, so don't immediately
                        // re-lock the session after setting/changing the PIN.
                        locked = false
                    },
                )
            }
        }
    }
}
