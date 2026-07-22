package app.lumadocs.kmp.data

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseUser(
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
)
