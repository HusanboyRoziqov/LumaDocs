package app.lumadocs.kmp

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data.Response
import app.lumadocs.kmp.utils.AuthError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthenticator(
    private val context: Context,
) : Authenticator {

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    override suspend fun login(): Response<FirebaseUser> {
        return try {
            Response.Loading(true)
            Log.d("GoogleAuth", "Starting login flow...")

            val googleIdOptions = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("13654041703-5mje82i21pmnkujr01cj8putbk06688p.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOptions)
                .build()

            Log.d("GoogleAuth", "Requesting credential...")
            // Credential Manager must launch its selector UI from an Activity context,
            // not the Application context, or it fails on Android 14+.
            val uiContext = LumaDocsApplication.currentActivity ?: context
            val result = credentialManager.getCredential(
                request = request,
                context = uiContext
            )

            Log.d("GoogleAuth", "Credential received: ${result.credential.type}")
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                Log.d("GoogleAuth", "Processing Google ID token...")
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d("GoogleAuth", "Signing in to Firebase...")
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val user = auth.signInWithCredential(authCredential).await().user
                Log.d("GoogleAuth", "Sign-in successful: ${user?.email}")
                Response.Success(
                    FirebaseUser(
                        userName = user?.displayName,
                        userEmail = user?.email,
                        userPhotoUrl = user?.photoUrl.toString()
                    )
                )
            } else {
                Log.e("GoogleAuth", "Invalid credential type: ${credential.type}")
                Response.Failure(AuthError.FAILED)
            }

        } catch (e: GetCredentialCancellationException) {
            Log.e("GoogleAuth", "User cancelled sign-in")
            Response.Failure(AuthError.CANCELLED)

        } catch (e: NoCredentialException) {
            Log.e("GoogleAuth", "No credentials available", e)
            Response.Failure(AuthError.NO_ACCOUNT)

        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "Credential error: ${e.type}", e)
            Response.Failure(AuthError.FAILED)

        } catch (e: Exception) {
            Log.e("GoogleAuth", "Unexpected error", e)
            Response.Failure(AuthError.FAILED)
        } finally {
            Response.Loading(false)
        }
    }

    override fun logOut() {
        auth.signOut()
    }

    override fun getCurrentUser(): FirebaseUser? {
        val currentUser = auth.currentUser ?: return null
        return FirebaseUser(
            userName = currentUser.displayName,
            userEmail = currentUser.email,
            userPhotoUrl = currentUser.photoUrl.toString()
        )
    }
}
