# LumaDocs — KMP Project Reference

> Fast reference for Claude Code. Read this before touching any file.

---

## What is LumaDocs?

A **Kotlin Multiplatform (KMP)** document vault app that targets **Android** and **iOS**.  
Users sign in with Google, connect Google Drive, and manage their personal files (images, PDFs, documents) organized by categories (Identity, Travel, Medical).  
UI is built entirely with **Compose Multiplatform (CMP)**.

---

## Project Structure

```
LumaDocs/
├── composeApp/                         # Single KMP module, contains all code
│   └── src/
│       ├── commonMain/                 # Shared Kotlin + Compose (runs on both platforms)
│       ├── androidMain/                # Android-specific implementations
│       └── iosMain/                    # iOS-specific implementations
├── iosApp/                             # Xcode project (thin wrapper, uses CocoaPods)
├── build.gradle.kts                    # Root build file
└── composeApp/build.gradle.kts        # Module build file (all dependencies here)
```

### Package root: `app.lumadocs.kmp`

---

## Source Layout — commonMain

| Path | Purpose |
|------|---------|
| `LumaDocs.kt` | Root composable — NavDisplay, backstack, route resolution |
| `navigation/OnboardingNavKey.kt` | Sealed `Route` interface (Onboarding, Started, Home, DocumentDetail) |
| `screens/OnboardingPager.kt` | Onboarding flow |
| `screens/StartedScreen.kt` | Google sign-in screen |
| `screens/home/HomeScreen.kt` | Bottom nav scaffold (Documents / Add / Settings tabs) |
| `screens/home/DocumentsScreen.kt` | File list with search, filter chips, shimmer loading, **ModalBottomSheet** for detail |
| `screens/home/DocumentDetailRoute.kt` | ViewModel wiring for detail (preview, save/share/delete); renders `DocumentInfoScreen` |
| `screens/home/DocumentInfoScreen.kt` | Full-screen single-file info page — top bar + ⋮ menu (Edit/Share/Delete); Edit opens a bottom sheet with name/desc/expiry fields |
| `screens/home/DocumentDetailScreen.kt` | Older combined detail layout — now used only by `FolderDetailRoute` (folder bottom sheet) |
| `screens/home/AddScreen.kt` | Upload screen (image picker + Drive upload) |
| `screens/home/SettingsScreen.kt` | Google Drive toggle, category management |
| `services/GoogleDriveRepository.kt` | Interface — `uploadFile`, `listFiles`, `deleteFile`, `getFileContent` |
| `data/FirebaseUser.kt` | User model (uid, name, email, photoUrl) |
| `data/Response.kt` | Sealed class: Loading / Success / Failure |
| `viewmodels/DocumentsViewModel.kt` | Files list state, refresh, categorize, updateMetadata, deleteFile |
| `viewmodels/DocumentDetailViewModel.kt` | Single-file state, loadPreview, updateMetadata, deleteCurrentFile, getShareLink |
| `viewmodels/SettingsViewModel.kt` | Google Drive connect/disconnect, category CRUD |
| `viewmodels/AddScreenViewModel.kt` | Upload flow |
| `viewmodels/StartScreenViewModel.kt` | Auth check on cold start |
| `di/Modules.kt` | Koin `commonModule` — all ViewModels registered as `viewModel {}` |
| `theme/LumaDocsTheme.kt` | MaterialTheme wrapper + all color tokens |
| `icons/` | Custom `ImageVector` icons (no external icon library) |
| `data_store/` | DataStore Preferences for locale + settings |
| `utils/ImageDecoder.kt` | `expect fun decodeImageBitmap(bytes)` |
| `utils/SecurityUtils.kt` | Encryption/decryption helpers |
| `platform/ShareUtils.kt` | `expect fun shareContent(...)` |

---

## Platform-Specific Files

### androidMain
| File | Purpose |
|------|---------|
| `MainActivity.kt` | Entry point, sets up Koin, calls `LumaDocs()` |
| `LumaDocsApplication.kt` | `Application` subclass — initializes Koin |
| `GoogleAuthenticator.kt` | Firebase + Google Sign-In (Credential Manager) |
| `services/GoogleDriveRepositoryImpl.kt` | Google Drive REST via `google-api-client-android` |
| `screens/home/AddScreenWrapper.kt` | Android image picker wrapper |
| `screens/home/DocumentsScreenPlatform.kt` | `actual fun openDriveFile(file)` |
| `di/Modules.android.kt` | `actual val platformModule` — provides `Authenticator`, `GoogleDriveRepository` |

### iosMain
| File | Purpose |
|------|---------|
| `MainViewController.kt` | `UIViewController` entry for SwiftUI bridge |
| `IosGoogleAuthenticator.kt` | GoogleSignIn CocoaPod-based auth |
| `services/IosGoogleDriveRepositoryImpl.kt` | Drive access via Ktor (Darwin engine) |
| `di/Modules.ios.kt` | `actual val platformModule` for iOS |

---

## Navigation

Uses **Jetbrains Navigation3** (`androidx.navigation3`).

```
Route.Onboarding  →  Route.Started  →  Route.Home(user)
                                             │
                                   (no longer navigates)
                                   DocumentDetail shown as ModalBottomSheet
                                   inside DocumentsScreen
```

**Important:** Tapping a single file navigates to `Route.DocumentDetail(fileId)`, a **full screen** rendered in `LumaDocs.kt` via `DocumentDetailRoute` → `DocumentInfoScreen` (top bar with name + ⋮ menu: Edit/Share/Delete; Edit opens a bottom sheet with name/description/expiry fields). Tapping a **folder** still opens a `ModalBottomSheet` (`FolderDetailRoute` → `DocumentDetailScreen`, the older combined layout).

---

## Data Flow

```
GoogleDriveRepository (interface)
        │
        └── platform impl (Android: google-api-client / iOS: Ktor)
                │
        DocumentsViewModel  ──────────────────────►  DocumentsScreen
        (StateFlow<DocumentsUiState>)                (collectAsState)
                │
        DocumentDetailViewModel ─────────────────►  DocumentDetailRoute
        (file, previewBytes, isLoading)              (collectAsState)
```

**DI:** Koin. ViewModels injected via `koinViewModel<T>()`. Repository injected via `KoinComponent.inject()` inside ViewModels.

---

## Key Models

```kotlin
// services/GoogleDriveRepository.kt
data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val description: String?,
    val createdTime: String?,      // ISO string, take(10) for date display
    val thumbnailLink: String?,
    val webViewLink: String?,
    val webContentLink: String?,
    val category: String?,         // set at upload time or categorized by name heuristic
    val encrypted: Boolean,
)
```

Auto-categorization in `DocumentsViewModel.categorizeFile()` matches file name keywords:
- `passport / id / identity / driver` → Identity
- `ticket / boarding / travel / trip` → Travel
- `vaccine / medical / health / prescription` → Medical
- else → Other

---

## Theme / Colors

All colors live in `theme/LumaDocsTheme.kt`. Never use raw hex in screens — always use these tokens:

| Token | Hex | Use |
|-------|-----|-----|
| `nBlack100` | `#11182C` | Main background |
| `nBlack400` | `#1A2238` (89% alpha) | Card / surface background |
| `nBlack500` | `#565E74` (44% alpha) | Muted text |
| `nWhite100` | `#F8F8F8` (86% alpha) | Primary text |
| `nWhite600` | `#8D9192` | Unselected chip color |
| `nBrand100` | `#07BCE8` | Accent / selected state / CTA buttons |
| `nBrand200` | `#649BBF` | Secondary accent |

---

## Dependencies (key ones)

| Library | Version alias | Purpose |
|---------|--------------|---------|
| Compose Multiplatform | `composeMultiplatform` | UI |
| Koin | `koin.core`, `koin.compose`, `koin.compose.viewmodel` | DI |
| Jetbrains Navigation3 | `jetbrains.navigation3.ui` | Navigation |
| Coil 3 | `coil.compose`, `coil.network.ktor` | Async image loading |
| Ktor | `ktor.client.core` (common), `okhttp`/`darwin` per platform | HTTP |
| DataStore | `androidx.datastore`, `androidx.datastore.preferences` | Local persistence |
| Firebase (Android only) | BOM, analytics, auth | Auth |
| Google Drive API (Android only) | `google.api.service.drive`, `google.api.client.android` | Drive |
| GoogleSignIn CocoaPod (iOS only) | pod `GoogleSignIn` | iOS auth |

---

## Rules & Conventions

### KMP / CMP
- All UI in `commonMain`. Platform-specific UI only if truly impossible in common.
- Use `expect`/`actual` pattern for platform APIs: image decoding, share sheet, file opening, DataStore path.
- Never import `android.*` or `UIKit` in `commonMain`.
- Compose resources (`Res.drawable.*`, `Res.string.*`) always from `commonMain/composeResources/`.

### Kotlin
- ViewModels extend `androidx.lifecycle.ViewModel` (works on both platforms via KMP lifecycle).
- Use `StateFlow` for UI state, `collectAsState()` in composables.
- Coroutines: `viewModelScope.launch` in ViewModels, `Dispatchers.Default` for CPU work (image decode), `Dispatchers.Main` for UI.
- Koin ViewModels: register with `viewModel { MyViewModel(get()) }` in module, inject with `koinViewModel<T>()` in composables.
- Data classes are in `data/` or next to the service they belong to (e.g., `DriveFile` in `services/`).

### Android
- `minSdk`: see `libs.versions.android.minSdk` in version catalog.
- `compileSdk` / `targetSdk`: from version catalog.
- JVM target: 11.
- Release build uses debug signing config (update before production).
- `isMinifyEnabled = false` on release — enable + add ProGuard rules before shipping.

### Compose
- Always use `@OptIn(ExperimentalMaterial3Api::class)` for BottomSheet and other experimental Material3 APIs.
- Bottom sheets: `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- Container background for sheets: `nBlack100`. Drag handle: `BottomSheetDefaults.DragHandle(color = nBlack400)`.
- Shimmer loading: `Modifier.shimmerEffect()` extension in `DocumentsScreen.kt`.
- File cards: `100.dp` height, `RoundedCornerShape(20.dp)`, `Color(0xFF1A2439)` background.
- `@Composable internal` for screen-level composables, `private` for sub-composables within the same file.

### Icons
- All icons are custom `ImageVector` objects in `icons/` package.
- No Material Icons Extended dependency — add new icons as custom vectors in the icons package.

---

## iOS Build

iOS uses CocoaPods. The framework is dynamic (`isStatic = false`).  
Deployment target: iOS 13.0.  
Build scripts at root: `build_ios.sh`, `rebuild_ios.sh`, `clean_xcode.sh`.  
After changing Kotlin code, run `./gradlew :composeApp:podInstall` before opening Xcode.

---

## What Does NOT Exist Yet

- Persistent metadata updates: **Android persists** title/description/expiry via `GoogleDriveRepository.updateFileMetadata` (detail-screen Save). **iOS is a stub** (returns false — no Drive impl yet).
- ProGuard / R8 rules for release
- Unit tests (test source sets exist but are empty)
- Categories are not persisted to DataStore (in-memory `SettingsViewModel` state only)
- `Route.DocumentDetail` is the live full-screen single-file detail (`DocumentInfoScreen`); only folder detail remains a bottom sheet
# LumaDocs
