package app.lumadocs.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Intercepts the system back gesture (Android back button / iOS edge swipe).
 *
 * Built on the same navigation-event dispatcher `NavDisplay` uses, so an enabled handler declared
 * inside a route always wins over the back-stack pop — and when [enabled] is false the event falls
 * through to `NavDisplay`, which pops one entry off the back stack.
 *
 * Call this unconditionally and drive it with [enabled]; wrapping it in an `if` changes the
 * composition order and makes which handler runs unpredictable.
 */
@Composable
fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = state, isBackEnabled = enabled, onBackCompleted = onBack)
}
