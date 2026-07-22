package app.lumadocs.kmp.data

import app.lumadocs.kmp.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
