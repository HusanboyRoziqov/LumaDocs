package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A sensitive PIN operation that must be confirmed with Google re-authentication. */
enum class PinFlowRequest { CREATE, CHANGE, DISABLE }

/**
 * Drives the full-screen PIN flow (re-auth → set/change/remove) from the app root, so it
 * covers the whole screen — including the bottom navigation — while it's open.
 */
object PinFlowState {
    private val _request = MutableStateFlow<PinFlowRequest?>(null)
    val request: StateFlow<PinFlowRequest?> = _request

    fun start(request: PinFlowRequest) { _request.value = request }
    fun clear() { _request.value = null }
}
