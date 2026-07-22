package app.lumadocs.kmp.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionECBMode
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.posix.size_tVar

/**
 * Must stay byte-for-byte compatible with the Android actual, which uses JCE
 * `Cipher.getInstance("AES")` — that resolves to AES/ECB/PKCS5Padding (PKCS5 and PKCS7
 * are identical for 16-byte blocks). Both sides share the same 32-byte key, so a file
 * encrypted on one platform decrypts on the other from the same Drive account.
 */
@OptIn(ExperimentalForeignApi::class)
actual object SecurityUtils {
    private val KEY = "LumaDocsSecretKeyLumaDocsSecretK".encodeToByteArray()

    private fun crypt(operation: UInt, data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        // Padding can push the output up to one block past the input.
        val capacity = data.size + kCCBlockSizeAES128.toInt()
        val output = ByteArray(capacity)

        memScoped {
            val written = alloc<size_tVar>()
            val status = KEY.usePinned { key ->
                data.usePinned { input ->
                    output.usePinned { out ->
                        CCCrypt(
                            operation,
                            kCCAlgorithmAES,
                            kCCOptionPKCS7Padding or kCCOptionECBMode,
                            key.addressOf(0), KEY.size.convert(),
                            null, // ECB takes no IV
                            input.addressOf(0), data.size.convert(),
                            out.addressOf(0), capacity.convert(),
                            written.ptr,
                        )
                    }
                }
            }
            check(status == kCCSuccess) { "CCCrypt failed with status $status" }
            return output.copyOf(written.value.toInt())
        }
    }

    actual fun encrypt(data: ByteArray): ByteArray = crypt(kCCEncrypt, data)

    actual fun decrypt(data: ByteArray): ByteArray = crypt(kCCDecrypt, data)
}
