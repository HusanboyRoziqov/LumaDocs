package app.lumadocs.kmp.platform

import android.widget.Toast
import app.lumadocs.kmp.LumaDocsApplication

actual fun showToast(message: String) {
    Toast.makeText(LumaDocsApplication.instance, message, Toast.LENGTH_SHORT).show()
}
