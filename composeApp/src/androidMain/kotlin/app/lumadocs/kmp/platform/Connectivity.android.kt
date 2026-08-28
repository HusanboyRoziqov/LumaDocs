package app.lumadocs.kmp.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.lumadocs.kmp.LumaDocsApplication

actual fun isOnline(): Boolean {
    val context = LumaDocsApplication.instance
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    // Only INTERNET is required. NET_CAPABILITY_VALIDATED reports false in ordinary situations —
    // right after a network switch, on VPNs, and on ROMs whose captive-portal probe is blocked —
    // which made the app refuse to sync and claim "no internet" while the connection worked. A
    // false negative blocks real work; a false positive just surfaces the request's own error.
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
