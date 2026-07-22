package app.lumadocs.kmp.utils

import app.lumadocs.kmp.platform.isOnline
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.error_no_google_account
import lumadocs.composeapp.generated.resources.error_sign_in_failed
import lumadocs.composeapp.generated.resources.no_internet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Turns raw exceptions / failure codes into short, friendly, localized messages so users
 * never see technical text like "Failed to launch the selector UI...".
 *
 * The suspend `getString` calls resolve against the JVM default locale, which the app's
 * language switch updates (see LocalAppLocale), so messages follow the chosen language.
 */
object ErrorMessages {

    /**
     * A user-facing message for a failed operation. When the device is offline we always
     * show the "no internet" message (the most likely real cause); otherwise the caller's
     * context-specific [fallback].
     */
    suspend fun forOperation(fallback: StringResource): String =
        if (!isOnline()) getString(Res.string.no_internet) else getString(fallback)

    /**
     * Maps an [AuthError] code from the authenticator to a localized message.
     * Returns null when the user simply cancelled — nothing should be shown then.
     */
    suspend fun forAuthCode(code: String): String? = when (code) {
        AuthError.CANCELLED -> null
        AuthError.NO_ACCOUNT -> getString(Res.string.error_no_google_account)
        else -> getString(Res.string.error_sign_in_failed)
    }
}

/** Stable, non-localized auth failure codes returned by the platform authenticator. */
object AuthError {
    const val CANCELLED = "cancelled"
    const val NO_ACCOUNT = "no_account"
    const val FAILED = "failed"
}
