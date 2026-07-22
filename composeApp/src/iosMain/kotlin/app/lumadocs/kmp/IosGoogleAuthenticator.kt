package app.lumadocs.kmp

import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data.Response
import app.lumadocs.kmp.services.DriveTokenProvider
import app.lumadocs.kmp.services.DriveTokenProvider.Companion.DRIVE_FILE_SCOPE
import app.lumadocs.kmp.utils.topViewController
import cocoapods.GoogleSignIn.GIDGoogleUser
import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosGoogleAuthenticator : Authenticator, DriveTokenProvider {

    init {
        restorePreviousSignIn()
    }

    private fun restorePreviousSignIn() {
        GIDSignIn.sharedInstance.restorePreviousSignInWithCompletion { _, _ -> }
    }

    private fun GIDGoogleUser.toFirebaseUser() = FirebaseUser(
        userName = profile?.name,
        userEmail = profile?.email,
        userPhotoUrl = profile?.imageURLWithDimension(128u)?.absoluteString,
    )

    override suspend fun login(): Response<FirebaseUser> =
        suspendCancellableCoroutine { cont ->
            val presenter = topViewController()
            if (presenter == null) {
                cont.resume(Response.Failure("Root UI view not available - app may not be fully initialized"))
                return@suspendCancellableCoroutine
            }

            // Ask for the Drive scope up front so the user consents once, rather than hitting
            // a second prompt the first time they open or upload a document.
            GIDSignIn.sharedInstance.signInWithPresentingViewController(
                presentingViewController = presenter,
                hint = null,
                additionalScopes = listOf(DRIVE_FILE_SCOPE),
            ) { result, error ->
                if (error != null) {
                    cont.resume(Response.Failure(error.localizedDescription ?: "Google sign-in failed"))
                    return@signInWithPresentingViewController
                }

                val googleUser = result?.user
                if (googleUser == null) {
                    cont.resume(Response.Failure("Google user or authentication is null"))
                    return@signInWithPresentingViewController
                }
                if (googleUser.idToken == null) {
                    cont.resume(Response.Failure("Missing ID token"))
                    return@signInWithPresentingViewController
                }

                cont.resume(Response.Success(googleUser.toFirebaseUser()))
            }
        }

    override fun logOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    override fun getCurrentUser(): FirebaseUser? =
        GIDSignIn.sharedInstance.currentUser?.toFirebaseUser()

    override suspend fun driveAccessToken(): String? {
        val user = GIDSignIn.sharedInstance.currentUser ?: return null
        // Accounts that signed in before the app asked for Drive access still have a valid
        // session but no Drive grant, so request it incrementally instead of failing.
        val granted = user.grantedScopes.orEmpty().contains(DRIVE_FILE_SCOPE)
        val authorized = if (granted) user else requestDriveScope(user) ?: return null
        return refreshedToken(authorized)
    }

    private suspend fun requestDriveScope(user: GIDGoogleUser): GIDGoogleUser? =
        suspendCancellableCoroutine { cont ->
            val presenter = topViewController()
            if (presenter == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            user.addScopes(
                scopes = listOf(DRIVE_FILE_SCOPE),
                presentingViewController = presenter,
            ) { result, _ -> cont.resume(result?.user) }
        }

    private suspend fun refreshedToken(user: GIDGoogleUser): String? =
        suspendCancellableCoroutine { cont ->
            user.refreshTokensIfNeededWithCompletion { refreshed, error ->
                cont.resume(if (error != null) null else refreshed?.accessToken?.tokenString)
            }
        }
}
