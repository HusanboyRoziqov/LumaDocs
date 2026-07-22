package app.lumadocs.kmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot signal raised after the user disconnects / wipes their session in
 * Settings. The app root ([LumaDocs]) observes this and resets the navigation
 * backstack to [app.lumadocs.kmp.navigation.Route.Onboarding], so the user lands
 * back on the first onboarding screen with a clean stack instead of relaunching
 * the app.
 */
object SessionResetState {
    private val _reset = MutableStateFlow(false)
    val reset: StateFlow<Boolean> = _reset

    fun trigger() { _reset.value = true }
    fun clear() { _reset.value = false }
}
