package app.lumadocs.kmp.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual object SecurityUtils {
    private const val ALGORITHM = "AES"

    private val KEY = "LumaDocsSecretKeyLumaDocsSecretK".toByteArray()

    actual fun encrypt(data: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(KEY, ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    actual fun decrypt(data: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(KEY, ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
}
