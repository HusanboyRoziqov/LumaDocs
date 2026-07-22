package app.lumadocs.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
