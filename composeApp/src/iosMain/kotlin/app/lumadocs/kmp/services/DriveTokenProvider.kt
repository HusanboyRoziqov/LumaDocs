package app.lumadocs.kmp.services

/**
 * Supplies an OAuth access token carrying the Drive scope, so the Drive repository does not
 * have to know how the user signed in.
 */
internal interface DriveTokenProvider {
    /**
     * A valid access token for [DRIVE_FILE_SCOPE], refreshing it (and prompting for consent
     * if the scope was never granted) when needed. Null when signed out or the user declined.
     */
    suspend fun driveAccessToken(): String?

    companion object {
        /**
         * Matches the Android side's DriveScopes.DRIVE_FILE: per-file access to what this app
         * creates, avoiding the restricted full-drive scope and its verification requirements.
         */
        const val DRIVE_FILE_SCOPE: String = "https://www.googleapis.com/auth/drive.file"
    }
}
