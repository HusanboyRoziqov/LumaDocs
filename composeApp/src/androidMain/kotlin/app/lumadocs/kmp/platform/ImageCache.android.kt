package app.lumadocs.kmp.platform

import app.lumadocs.kmp.LumaDocsApplication

actual fun imageCacheDir(): String = LumaDocsApplication.instance.cacheDir.absolutePath
