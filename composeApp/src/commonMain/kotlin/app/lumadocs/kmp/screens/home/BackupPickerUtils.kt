package app.lumadocs.kmp.screens.home

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberBackupFilePicker(onResult: (uriString: String?) -> Unit): () -> Unit
