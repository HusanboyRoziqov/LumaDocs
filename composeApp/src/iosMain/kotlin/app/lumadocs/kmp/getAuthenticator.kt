package app.lumadocs.kmp

import org.koin.mp.KoinPlatform.getKoin

actual fun getAuthenticator(): Authenticator {
    return getKoin().get<Authenticator>()
}
