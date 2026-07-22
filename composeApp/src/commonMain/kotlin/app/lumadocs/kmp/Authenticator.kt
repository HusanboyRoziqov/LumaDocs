package app.lumadocs.kmp

import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data.Response

interface Authenticator {
    suspend fun login(): Response<FirebaseUser>
    fun logOut()
    fun getCurrentUser(): FirebaseUser?
}

expect fun getAuthenticator(): Authenticator
