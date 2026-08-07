package app.lumadocs.kmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.lumadocs.kmp.services.DriveFile
import lumadocs.composeapp.generated.resources.Res
import lumadocs.composeapp.generated.resources.cat_education
import lumadocs.composeapp.generated.resources.cat_financial
import lumadocs.composeapp.generated.resources.cat_identity
import lumadocs.composeapp.generated.resources.cat_legal
import lumadocs.composeapp.generated.resources.cat_medical
import lumadocs.composeapp.generated.resources.cat_other
import lumadocs.composeapp.generated.resources.cat_travel
import lumadocs.composeapp.generated.resources.months_short
import lumadocs.composeapp.generated.resources.type_document
import lumadocs.composeapp.generated.resources.type_file
import lumadocs.composeapp.generated.resources.type_image
import lumadocs.composeapp.generated.resources.type_pdf
import lumadocs.composeapp.generated.resources.type_presentation
import lumadocs.composeapp.generated.resources.type_spreadsheet
import lumadocs.composeapp.generated.resources.unit_kb
import lumadocs.composeapp.generated.resources.unit_mb
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

/**
 * The document categories from the design prototype (`tokens.jsx` → `LUMA.cat`), each with a
 * hue and a subtle tinted-surface color.
 */
enum class DocCategory(
    val key: String,
    /** English label — for non-composable use (search indexing, logs). UI uses [labelRes]. */
    val label: String,
    val labelRes: StringResource,
    val hue: Color,
    val tint: Color,
) {
    IDENTITY("identity", "Identity", Res.string.cat_identity, Color(0xFF6D89D4), Color(0x246D89D4)),
    TRAVEL("travel", "Travel", Res.string.cat_travel, Color(0xFFE8B468), Color(0x24E8B468)),
    MEDICAL("medical", "Medical", Res.string.cat_medical, Color(0xFFB77E8C), Color(0x24B77E8C)),
    FINANCIAL("financial", "Financial", Res.string.cat_financial, Color(0xFF7FB77E), Color(0x247FB77E)),
    EDUCATION("education", "Education", Res.string.cat_education, Color(0xFFC9A870), Color(0x24C9A870)),
    LEGAL("legal", "Legal", Res.string.cat_legal, Color(0xFF8A8FA5), Color(0x248A8FA5)),
    OTHER("other", "Other", Res.string.cat_other, Color(0xFFA0A0A0), Color(0x1FA0A0A0));

    companion object {
        fun fromKey(key: String?): DocCategory =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: OTHER
    }
}

/**
 * Resolves a file's category. Uses the stored [DriveFile.category] when present, otherwise falls
 * back to the name-keyword heuristic (mirrors the app's original `categorizeFile`).
 */
fun categoryOf(file: DriveFile): DocCategory {
    file.category?.takeIf { it.isNotBlank() }?.let { stored ->
        val direct = DocCategory.entries.firstOrNull { it.key.equals(stored.trim(), ignoreCase = true) }
        if (direct != null) return direct
    }
    val n = file.name.lowercase()
    return when {
        listOf("passport", "id", "identity", "driver", "license", "birth").any { n.contains(it) } -> DocCategory.IDENTITY
        listOf("ticket", "boarding", "travel", "trip", "visa", "entry").any { n.contains(it) } -> DocCategory.TRAVEL
        listOf("vaccine", "medical", "health", "prescription", "insurance").any { n.contains(it) } -> DocCategory.MEDICAL
        listOf("tax", "invoice", "bank", "return", "1040", "financial").any { n.contains(it) } -> DocCategory.FINANCIAL
        listOf("diploma", "degree", "transcript", "certificate of").any { n.contains(it) } -> DocCategory.EDUCATION
        listOf("lease", "contract", "legal", "marriage", "deed").any { n.contains(it) } -> DocCategory.LEGAL
        else -> DocCategory.OTHER
    }
}

/** The category name in the user's language — always use this for anything on screen. */
@Composable
fun DocCategory.localizedLabel(): String = stringResource(labelRes)

/** The file-type label ("Image", "PDF"…) in the user's language. */
@Composable
fun localizedMimeLabel(mimeType: String): String = stringResource(
    when {
        mimeType.contains("image", ignoreCase = true) -> Res.string.type_image
        mimeType.contains("pdf", ignoreCase = true) -> Res.string.type_pdf
        mimeType.contains("document", ignoreCase = true) -> Res.string.type_document
        mimeType.contains("word", ignoreCase = true) -> Res.string.type_document
        mimeType.contains("sheet", ignoreCase = true) -> Res.string.type_spreadsheet
        mimeType.contains("presentation", ignoreCase = true) -> Res.string.type_presentation
        else -> Res.string.type_file
    }
)

/** Days until the file's expiry, or null if it has no parseable expiry date. Negative = past. */
@OptIn(kotlin.time.ExperimentalTime::class)
fun expiryDaysOf(file: DriveFile): Int? {
    val date = parseExpiry(file.expiryDate) ?: return null
    val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
    return today.daysUntil(date)
}

/** Parses a stored expiry string. Accepts ISO `yyyy-MM-dd` (optionally with a time suffix). */
fun parseExpiry(raw: String?): LocalDate? {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return null
    // ISO date, possibly the leading part of a full timestamp.
    runCatching { return LocalDate.parse(s.take(10)) }
    return null
}

/** Human-readable expiry, e.g. "Aug 12, 2026". Falls back to the raw string if unparseable. */
fun formatExpiry(raw: String?): String {
    val date = parseExpiry(raw) ?: return raw?.trim().orEmpty()
    val mon = when (date.month) {
        Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
        Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
        else -> ""
    }
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "$mon $day, ${date.year}"
}

/**
 * Localized expiry, e.g. "Aug 12, 2026" / "авг 12, 2026". Month names come from the string array so
 * the date reads naturally in whichever language the app is running in.
 */
@Composable
fun formatExpiryLocalized(raw: String?): String {
    val date = parseExpiry(raw) ?: return raw?.trim().orEmpty()
    val months = stringArrayResource(Res.array.months_short)
    val mon = months.getOrNull(date.month.ordinal).orEmpty()
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "$mon $day, ${date.year}"
}

/**
 * Rewrites a Google Drive thumbnail URL to request a specific pixel size (`=sNNN`). Smaller sizes
 * download and decode faster — use small sizes for list cells. Returns the link unchanged if it has
 * no size segment.
 */
fun sizedThumb(link: String?, px: Int): String? {
    if (link.isNullOrBlank()) return link
    val re = Regex("=s\\d+")
    return if (re.containsMatchIn(link)) link.replace(re, "=s$px") else link
}

/** Localized byte count — "2.4 MB" in English, "2,4 МБ" in Russian. */
@Composable
fun formatSizeLocalized(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    val kb = bytes / 1024.0
    return if (kb < 1024) stringResource(Res.string.unit_kb, kb.roundTo(0))
    else stringResource(Res.string.unit_mb, (kb / 1024).roundTo(1))
}

/** Formats a byte count like Drive's `size` into "2.4 MB" / "960 KB". Non-composable fallback. */
fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "—"
    val kb = bytes / 1024.0
    return if (kb < 1024) "${kb.roundTo(0)} KB" else "${(kb / 1024).roundTo(1)} MB"
}

private fun Double.roundTo(decimals: Int): String {
    if (decimals == 0) return this.toLong().toString()
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val rounded = kotlin.math.round(this * factor) / factor
    return rounded.toString()
}
