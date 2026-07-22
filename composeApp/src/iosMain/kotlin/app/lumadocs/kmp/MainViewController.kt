package app.lumadocs.kmp

import androidx.compose.ui.window.ComposeUIViewController
import app.lumadocs.kmp.di.initKoin
import org.koin.core.context.stopKoin

fun MainViewController() = ComposeUIViewController(
    configure = {

        try {
            initKoin()
        } catch (e: Exception) {

        }
    }
) {
    LumaDocs()
}
