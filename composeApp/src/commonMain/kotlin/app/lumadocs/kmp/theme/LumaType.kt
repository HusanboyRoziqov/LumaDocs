package app.lumadocs.kmp.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography for the candlelight design.
 *
 * The prototype uses three web fonts:
 *   - Instrument Serif  → [LumaDisplay]  (large italic display headings)
 *   - Inter             → [LumaUi]       (body / UI)
 *   - JetBrains Mono    → [LumaMono]     (kickers, codes, meta)
 *
 * No font files are bundled yet, so these map to the platform generic families as a faithful
 * fallback (serif / sans / monospace). When the real TTFs are added under
 * `composeResources/font/`, swap these three vals to `FontFamily(Font(Res.font.…))`.
 */
val LumaDisplay: FontFamily = FontFamily.Serif
val LumaUi: FontFamily = FontFamily.SansSerif
val LumaMono: FontFamily = FontFamily.Monospace

/** Recurring text styles, named after their role in the prototype. */
object LumaText {
    /** Large italic serif hero titles (36–46sp in the mock). */
    val display = TextStyle(
        fontFamily = LumaDisplay,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 36.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp,
    )

    /** Uppercase mono kicker above a section/title. */
    val kicker = TextStyle(
        fontFamily = LumaMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 1.6.sp,
    )

    /** Mono meta text (codes, sizes, expiry counts). */
    val meta = TextStyle(
        fontFamily = LumaMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    )

    val title = TextStyle(
        fontFamily = LumaUi,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    )

    val body = TextStyle(
        fontFamily = LumaUi,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )

    val label = TextStyle(
        fontFamily = LumaUi,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = (-0.1).sp,
    )
}

/** Material typography wired to the candlelight families so stray Material text stays on-brand. */
fun LumaTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = LumaDisplay),
        displayMedium = base.displayMedium.copy(fontFamily = LumaDisplay),
        displaySmall = base.displaySmall.copy(fontFamily = LumaDisplay),
        headlineLarge = base.headlineLarge.copy(fontFamily = LumaUi),
        headlineMedium = base.headlineMedium.copy(fontFamily = LumaUi),
        headlineSmall = base.headlineSmall.copy(fontFamily = LumaUi),
        titleLarge = base.titleLarge.copy(fontFamily = LumaUi),
        titleMedium = base.titleMedium.copy(fontFamily = LumaUi),
        titleSmall = base.titleSmall.copy(fontFamily = LumaUi),
        bodyLarge = base.bodyLarge.copy(fontFamily = LumaUi),
        bodyMedium = base.bodyMedium.copy(fontFamily = LumaUi),
        bodySmall = base.bodySmall.copy(fontFamily = LumaUi),
        labelLarge = base.labelLarge.copy(fontFamily = LumaUi),
        labelMedium = base.labelMedium.copy(fontFamily = LumaUi),
        labelSmall = base.labelSmall.copy(fontFamily = LumaMono),
    )
}
