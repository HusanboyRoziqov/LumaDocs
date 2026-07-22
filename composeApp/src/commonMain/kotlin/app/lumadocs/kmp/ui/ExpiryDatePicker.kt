package app.lumadocs.kmp.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.lumadocs.kmp.theme.LocalLumaColors
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Candlelight expiry date picker shared by the document edit sheet and the scan save form.
 * Only allows dates from today onward; emits the picked date as an ISO `yyyy-MM-dd` string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDatePickerDialog(
    currentIso: String?,
    onPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLumaColors.current
    val todayUtc = remember { startOfTodayUtcMillis() }
    val currentYear = remember {
        Instant.fromEpochMilliseconds(todayUtc).toLocalDateTime(TimeZone.UTC).year
    }
    val initial = remember(currentIso) { (currentIso?.let { isoToUtcMillis(it) }?.takeIf { it >= todayUtc }) ?: todayUtc }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        yearRange = currentYear..(currentYear + 20),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            override fun isSelectableYear(year: Int) = year >= currentYear
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = c.bg2, titleContentColor = c.textDim, headlineContentColor = c.text,
        weekdayContentColor = c.textDim, navigationContentColor = c.text, yearContentColor = c.text,
        currentYearContentColor = c.accent, selectedYearContentColor = Color(0xFF1A1408), selectedYearContainerColor = c.accent,
        dayContentColor = c.text, selectedDayContentColor = Color(0xFF1A1408), selectedDayContainerColor = c.accent,
        todayContentColor = c.accent, todayDateBorderColor = c.accent,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onPicked(utcMillisToIso(it)) } }) { Text("OK", color = c.accent, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textDim) } },
        colors = colors,
    ) { DatePicker(state = state, colors = colors, title = null) }
}

private fun utcMillisToIso(millis: Long): String =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString()

private fun isoToUtcMillis(iso: String): Long? = try {
    LocalDate.parse(iso.take(10)).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (e: Exception) { null }

@OptIn(kotlin.time.ExperimentalTime::class)
private fun startOfTodayUtcMillis(): Long =
    kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.UTC).date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
