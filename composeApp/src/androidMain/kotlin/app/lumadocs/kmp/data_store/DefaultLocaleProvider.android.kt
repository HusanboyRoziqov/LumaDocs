package app.lumadocs.kmp.data_store

import java.util.Locale

actual fun getDefaultLocale(): String {
    return Locale.getDefault().toString()
}
