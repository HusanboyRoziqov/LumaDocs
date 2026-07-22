package app.lumadocs.kmp.ui

import androidx.compose.ui.graphics.Color
import app.lumadocs.kmp.services.DriveFile
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
    val label: String,
    val hue: Color,
    val tint: Color,
) {
    IDENTITY("identity", "Identity", Color(0xFF6D89D4), Color(0x246D89D4)),
    TRAVEL("travel", "Travel", Color(0xFFE8B468), Color(0x24E8B468)),
    MEDICAL("medical", "Medical", Color(0xFFB77E8C), Color(0x24B77E8C)),
    FINANCIAL("financial", "Financial", Color(0xFF7FB77E), Color(0x247FB77E)),
    EDUCATION("education", "Education", Color(0xFFC9A870), Color(0x24C9A870)),
    LEGAL("legal", "Legal", Color(0xFF8A8FA5), Color(0x248A8FA5)),
    OTHER("other", "Other", Color(0xFFA0A0A0), Color(0x1FA0A0A0));

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
 * Rewrites a Google Drive thumbnail URL to request a specific pixel size (`=sNNN`). Smaller sizes
 * download and decode faster — use small sizes for list cells. Returns the link unchanged if it has
 * no size segment.
 */
fun sizedThumb(link: String?, px: Int): String? {
    if (link.isNullOrBlank()) return link
    val re = Regex("=s\\d+")
    return if (re.containsMatchIn(link)) link.replace(re, "=s$px") else link
}

/** Formats a byte count like Drive's `size` into "2.4 MB" / "960 KB". */
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
