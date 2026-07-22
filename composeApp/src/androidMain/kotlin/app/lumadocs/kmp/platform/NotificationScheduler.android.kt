package app.lumadocs.kmp.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.lumadocs.kmp.ExpiryNotificationReceiver
import app.lumadocs.kmp.LumaDocsApplication
import kotlinx.datetime.LocalDate
import java.util.Calendar

private data class Reminder(
    val offsetDays: Int,
    val hourOfDay: Int,
    val minute: Int = 0,
    val kind: String,
)

private val REMINDERS = listOf(
    Reminder(30, 9, 0, "month"),
    Reminder(7, 9, 0, "week"),
    Reminder(1, 9, 0, "tomorrow"),
    Reminder(0, 9, 0, "today"),
    Reminder(0, 17, 0, "today"),
)

actual fun scheduleExpiryNotifications(fileId: String, fileName: String, expiryDateIso: String) {
    val context = LumaDocsApplication.instance
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val date = runCatching { LocalDate.parse(expiryDateIso) }.getOrNull() ?: return
    val year = date.year
    val month = date.monthNumber
    val day = date.dayOfMonth

    val now = System.currentTimeMillis()

    var scheduledCount = 0

    REMINDERS.forEachIndexed { index, reminder ->
        val triggerAt =
            if (reminder.offsetDays == 0 && reminder.hourOfDay == 0 && reminder.minute > 0) {
                now + reminder.minute * 60_000L
            } else {
                val cal = Calendar.getInstance().apply {
                    set(year, month - 1, day, reminder.hourOfDay, reminder.minute, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -reminder.offsetDays)
                }
                cal.timeInMillis
            }
        if (triggerAt <= now) return@forEachIndexed

        scheduleAlarm(context, alarmManager, fileId, fileName, index, triggerAt, reminder.kind)
        scheduledCount++
    }

    if (scheduledCount == 0) {

        val endOfExpiryDay = Calendar.getInstance().apply {
            set(year, month - 1, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val kind = if (endOfExpiryDay < now) "expired" else "today"
        val triggerAt = now + 5_000L
        scheduleAlarm(context, alarmManager, fileId, fileName, REMINDERS.size, triggerAt, kind)
    }
}

private fun scheduleAlarm(
    context: Context,
    alarmManager: AlarmManager,
    fileId: String,
    fileName: String,
    index: Int,
    triggerAt: Long,
    kind: String,
) {
    val intent = Intent(context, ExpiryNotificationReceiver::class.java).apply {
        putExtra("fileId", fileId)
        putExtra("fileName", fileName)
        putExtra("kind", kind)
    }
    val requestCode = requestCodeFor(fileId, index)
    val pending = PendingIntent.getBroadcast(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val canExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else true

    if (canExact) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    } else {

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }
}

actual fun cancelExpiryNotifications(fileId: String) {
    val context = LumaDocsApplication.instance
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    (0..REMINDERS.size).forEach { index ->
        val intent = Intent(context, ExpiryNotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, requestCodeFor(fileId, index), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let { alarmManager.cancel(it) }
    }
}

private fun requestCodeFor(fileId: String, index: Int): Int =
    (fileId.hashCode() and 0x00FFFFFF) * 10 + index
