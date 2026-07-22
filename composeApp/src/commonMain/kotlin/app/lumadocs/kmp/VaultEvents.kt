package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lightweight cross-screen signal: bumped when something (e.g. a scan-save) changes the vault
 * contents from a different nav entry, so the Home vault list can refresh itself.
 */
object VaultEvents {
    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick

    fun requestRefresh() {
        _refreshTick.value = _refreshTick.value + 1
    }
}
