package app.lumadocs.kmp.platform

import app.lumadocs.kmp.data_store.getDefaultLocale
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSinceNow
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private data class Reminder(
    val offsetDays: Int,
    val hourOfDay: Int,
    val minute: Int = 0,
    val kind: String,
)

/** Kept in step with the Android actual so both platforms remind at the same moments. */
private val REMINDERS = listOf(
    Reminder(30, 9, 0, "month"),
    Reminder(7, 9, 0, "week"),
    Reminder(1, 9, 0, "tomorrow"),
    Reminder(0, 9, 0, "today"),
    Reminder(0, 17, 0, "today"),
)

private fun identifier(fileId: String, index: Int) = "lumadocs-expiry-$fileId-$index"

/**
 * Unlike Android — which localizes in the receiver at delivery time — iOS notification text
 * is fixed when the request is registered. Documents scheduled before a language change keep
 * the old language until their expiry is edited and rescheduled.
 */
private fun currentLanguage(): String {
    val selected = NSUserDefaults.standardUserDefaults
        .stringArrayForKey("AppleLanguages")
        ?.firstOrNull() as? String
    return if ((selected ?: getDefaultLocale()).startsWith("ru")) "ru" else "en"
}

private fun buildExpiryText(kind: String, fileName: String, lang: String): Pair<String, String> {
    return if (lang == "ru") when (kind) {
        "month" -> "⏳ Истекает через месяц" to
            "Срок действия «$fileName» истекает через 1 месяц. Позаботьтесь о продлении заранее."

        "week" -> "⏳ Истекает через неделю" to
            "Срок действия «$fileName» истекает через 1 неделю. Не забудьте продлить документ."

        "tomorrow" -> "⚠️ Истекает завтра" to
            "Срок действия «$fileName» истекает завтра. Успейте продлить его вовремя."

        "expired" -> "❌ Срок истёк" to
            "Срок действия «$fileName» уже истёк. Пожалуйста, продлите его как можно скорее."

        else -> "🔴 Истекает сегодня" to
            "Срок действия «$fileName» истекает сегодня! Продлите его до конца дня."
    } else when (kind) {
        "month" -> "⏳ Expires in 1 month" to
            "\"$fileName\" will expire in 1 month. Consider renewing it soon so it doesn't lapse."

        "week" -> "⏳ Expires in 1 week" to
            "\"$fileName\" expires in 1 week. Don't forget to renew it in time."

        "tomorrow" -> "⚠️ Expires tomorrow" to
            "\"$fileName\" expires tomorrow. Make sure to renew it before it's too late."

        "expired" -> "❌ Document expired" to
            "\"$fileName\" has already expired. Please renew it as soon as possible."

        else -> "🔴 Expires today" to
            "\"$fileName\" expires today! Renew it before the day ends."
    }
}

private fun componentsFor(date: LocalDate, hour: Int, minute: Int) = NSDateComponents().apply {
    setYear(date.year.toLong())
    setMonth(date.monthNumber.toLong())
    setDay(date.dayOfMonth.toLong())
    setHour(hour.toLong())
    setMinute(minute.toLong())
    setSecond(0)
}

private fun submit(
    fileId: String,
    fileName: String,
    index: Int,
    kind: String,
    trigger: UNNotificationTrigger,
) {
    val (title, message) = buildExpiryText(kind, fileName, currentLanguage())
    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(message)
        setSound(UNNotificationSound.defaultSound())
        // Read by the app when the user taps the notification, to open this document.
        setUserInfo(mapOf("openFileId" to fileId))
    }
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = identifier(fileId, index),
        content = content,
        trigger = trigger,
    )
    UNUserNotificationCenter.currentNotificationCenter()
        .addNotificationRequest(request) { error ->
            if (error != null) {
                println("NotificationScheduler: failed to add request: ${error.localizedDescription}")
            }
        }
}

actual fun scheduleExpiryNotifications(fileId: String, fileName: String, expiryDateIso: String) {
    val expiry = runCatching { LocalDate.parse(expiryDateIso) }.getOrNull() ?: return

    // Re-scheduling the same file should replace its reminders, not stack a second set.
    cancelExpiryNotifications(fileId)

    var scheduled = 0
    REMINDERS.forEachIndexed { index, reminder ->
        val fireDate = expiry.minus(DatePeriod(days = reminder.offsetDays))
        val components = componentsFor(fireDate, reminder.hourOfDay, reminder.minute)
        val fireAt = NSCalendar.currentCalendar.dateFromComponents(components) ?: return@forEachIndexed
        if (fireAt.timeIntervalSinceNow <= 0.0) return@forEachIndexed

        submit(
            fileId, fileName, index, reminder.kind,
            UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false),
        )
        scheduled++
    }

    if (scheduled == 0) {
        // Every reminder is in the past, so tell the user where the document stands now
        // rather than staying silent.
        val endOfDay = NSCalendar.currentCalendar
            .dateFromComponents(componentsFor(expiry, 23, 59))
        val kind = if (endOfDay != null && endOfDay.timeIntervalSinceNow < 0.0) "expired" else "today"
        submit(
            fileId, fileName, REMINDERS.size, kind,
            UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(5.0, repeats = false),
        )
    }
}

actual fun cancelExpiryNotifications(fileId: String) {
    val identifiers = (0..REMINDERS.size).map { identifier(fileId, it) }
    UNUserNotificationCenter.currentNotificationCenter().apply {
        removePendingNotificationRequestsWithIdentifiers(identifiers)
        removeDeliveredNotificationsWithIdentifiers(identifiers)
    }
}
