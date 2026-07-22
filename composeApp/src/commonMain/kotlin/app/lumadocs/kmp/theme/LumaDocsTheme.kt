package app.lumadocs.kmp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * LumaDocs "candlelight vault" palette.
 *
 * The luma-prefixed fields are the canonical design tokens (ported 1:1 from the design
 * prototype's tokens.jsx). The legacy nBlack / nWhite / nBrand fields are kept so the many
 * auxiliary screens that already reference them keep compiling — their values are remapped onto
 * the new palette so those screens inherit the candlelight look for free.
 */
@Immutable
data class LumaColors(
    // ── Canonical candlelight tokens ──────────────────────────
    val bg: Color,
    val bg2: Color,
    val bg3: Color,
    val hairline: Color,
    val hairline2: Color,
    val text: Color,
    val textDim: Color,
    val textMute: Color,
    val accent: Color,
    val accentHi: Color,
    val accentDim: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,

    // ── Legacy accessors (remapped onto the palette above) ────
    val nBlack100: Color,
    val nBlack200: Color,
    val nBlack300: Color,
    val nBlack400: Color,
    val nBlack500: Color,
    val nWhite100: Color,
    val nWhite600: Color,
    val nWhite800: Color,
    val nBrand100: Color,
    val nBrand200: Color,
    val nIconChip: Color,
    val nShimmer: Color,
    val nShimmerHi: Color,
)

private val CandlelightColors = LumaColors(
    bg = Color(0xFF0B0D12),
    bg2 = Color(0xFF11141B),
    bg3 = Color(0xFF181C26),
    hairline = Color(0x0FFFFFFF),   // white @ 6%
    hairline2 = Color(0x1AFFFFFF),  // white @ 10%
    text = Color(0xFFF5F3EE),
    textDim = Color(0x9EF5F3EE),    // bone @ 62%
    textMute = Color(0x61F5F3EE),   // bone @ 38%
    accent = Color(0xFFE8B468),
    accentHi = Color(0xFFF2C98A),
    accentDim = Color(0x2EE8B468),  // amber @ 18%
    ok = Color(0xFF7FB77E),
    warn = Color(0xFFE89B5D),
    err = Color(0xFFD96861),

    // Legacy remap
    nBlack100 = Color(0xFF0B0D12),  // main bg
    nBlack200 = Color(0xFF11141B),
    nBlack300 = Color(0x9EF5F3EE),  // textDim
    nBlack400 = Color(0xFF11141B),  // card / surface
    nBlack500 = Color(0x61F5F3EE),  // textMute
    nWhite100 = Color(0xFFF5F3EE),  // primary text
    nWhite600 = Color(0x9EF5F3EE),  // unselected / secondary text
    nWhite800 = Color(0x1AFFFFFF),  // hairline
    nBrand100 = Color(0xFFE8B468),  // accent
    nBrand200 = Color(0xFFF2C98A),  // accent hi
    nIconChip = Color(0xFF181C26),  // raised chip bg
    nShimmer = Color(0xFF11141B),
    nShimmerHi = Color(0xFF181C26),
)

val LocalLumaColors = staticCompositionLocalOf { CandlelightColors }

// ── Canonical token accessors ─────────────────────────────────
val lumaBg: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.bg
val lumaBg2: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.bg2
val lumaBg3: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.bg3
val lumaHairline: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.hairline
val lumaHairline2: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.hairline2
val lumaText: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.text
val lumaTextDim: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.textDim
val lumaTextMute: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.textMute
val lumaAccent: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.accent
val lumaAccentHi: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.accentHi
val lumaAccentDim: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.accentDim
val lumaOk: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.ok
val lumaWarn: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.warn
val lumaErr: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.err

// ── Legacy accessors ──────────────────────────────────────────
val nBlack100: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBlack100
val nBlack200: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBlack200
val nBlack300: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBlack300
val nBlack400: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBlack400
val nBlack500: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBlack500
val nWhite100: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nWhite100
val nWhite600: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nWhite600
val nWhite800: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nWhite800
val nBrand100: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBrand100
val nBrand200: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nBrand200
val nIconChip: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nIconChip
val nShimmer: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nShimmer
val nShimmerHi: Color @Composable @ReadOnlyComposable get() = LocalLumaColors.current.nShimmerHi

private val CandlelightMaterialColors = darkColorScheme(
    primary = Color(0xFFE8B468),
    onPrimary = Color(0xFF1A1408),
    secondary = Color(0xFFF2C98A),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF11141B),
    onBackground = Color(0xFFF5F3EE),
    onSurface = Color(0xFFF5F3EE),
    error = Color(0xFFD96861),
)

/**
 * The mock is a dark-only "vault" design, so both light and dark resolve to the same
 * candlelight scheme. [isDarkTheme] is retained for call-site compatibility.
 */
@Composable
internal fun LumaDocsTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLumaColors provides CandlelightColors) {
        MaterialTheme(
            colorScheme = CandlelightMaterialColors,
            typography = LumaTypography(),
            content = content,
        )
    }
}
