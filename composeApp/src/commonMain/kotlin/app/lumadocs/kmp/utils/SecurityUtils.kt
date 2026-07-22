package app.lumadocs.kmp.utils

expect object SecurityUtils {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}
