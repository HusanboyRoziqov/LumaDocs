package app.lumadocs.kmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ExpiryNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val fileId = intent.getStringExtra("fileId") ?: return
        val fileName = intent.getStringExtra("fileName") ?: "Document"
        val kind = intent.getStringExtra("kind") ?: "today"

        val lang = runCatching {
            runBlocking {
                LumaDocsApplication.dataStore.data.first()[stringPreferencesKey("languageCode")]
            }
        }.getOrNull() ?: "en"

        val (title, message) = buildExpiryText(kind, fileName, if (lang == "ru") "ru" else "en")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm, context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openFileId", fileId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context, fileId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF07BCE8.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(fileId.hashCode(), notification)
    }

    private fun buildExpiryText(kind: String, fileName: String, lang: String): Pair<String, String> {
        return if (lang == "ru") when (kind) {
            "month"    -> "⏳ Истекает через месяц" to
                "Срок действия «$fileName» истекает через 1 месяц. Позаботьтесь о продлении заранее."
            "week"     -> "⏳ Истекает через неделю" to
                "Срок действия «$fileName» истекает через 1 неделю. Не забудьте продлить документ."
            "tomorrow" -> "⚠️ Истекает завтра" to
                "Срок действия «$fileName» истекает завтра. Успейте продлить его вовремя."
            "expired"  -> "❌ Срок истёк" to
                "Срок действия «$fileName» уже истёк. Пожалуйста, продлите его как можно скорее."
            else       -> "🔴 Истекает сегодня" to
                "Срок действия «$fileName» истекает сегодня! Продлите его до конца дня."
        } else when (kind) {
            "month"    -> "⏳ Expires in 1 month" to
                "\"$fileName\" will expire in 1 month. Consider renewing it soon so it doesn't lapse."
            "week"     -> "⏳ Expires in 1 week" to
                "\"$fileName\" expires in 1 week. Don't forget to renew it in time."
            "tomorrow" -> "⚠️ Expires tomorrow" to
                "\"$fileName\" expires tomorrow. Make sure to renew it before it's too late."
            "expired"  -> "❌ Document expired" to
                "\"$fileName\" has already expired. Please renew it as soon as possible."
            else       -> "🔴 Expires today" to
                "\"$fileName\" expires today! Renew it before the day ends."
        }
    }

    private fun ensureChannel(nm: NotificationManager, context: Context) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Document Expiry",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Reminders before document expiry dates" }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "lumadocs_expiry"
    }
}
