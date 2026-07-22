package app.lumadocs.kmp.platform

import androidx.compose.runtime.Composable

/**
 * Applies the correct status/navigation bar icon contrast for the current app
 * theme. The bars themselves are transparent (edge-to-edge), so their colour is
 * whatever the app draws behind them; this only controls whether the system
 * icons are light or dark. Follows the in-app theme, not the device setting.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
