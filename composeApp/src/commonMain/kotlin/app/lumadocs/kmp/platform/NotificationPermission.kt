package app.lumadocs.kmp.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit
