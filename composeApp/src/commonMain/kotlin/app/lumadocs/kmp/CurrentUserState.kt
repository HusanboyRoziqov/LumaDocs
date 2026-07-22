package app.lumadocs.kmp

import app.lumadocs.kmp.data.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Live, app-wide source of truth for the signed-in user.
 *
 * The navigation key [app.lumadocs.kmp.navigation.Route.Home] captures the user
 * only at navigation time, so screens that read it directly go stale when the
 * user signs in or out *without* a new Home entry being pushed (e.g. from the
 * Settings tab). Screens observe this flow instead so they react immediately:
 *  - set on sign-in (Started screen or Settings "Login with Google")
 *  - cleared on disconnect / session wipe
 */
object CurrentUserState {
    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user

    fun set(user: FirebaseUser?) { _user.value = user }
    fun clear() { _user.value = null }
}
