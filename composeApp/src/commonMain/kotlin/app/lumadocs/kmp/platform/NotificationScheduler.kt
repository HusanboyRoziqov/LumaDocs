package app.lumadocs.kmp.platform

expect fun scheduleExpiryNotifications(fileId: String, fileName: String, expiryDateIso: String)

expect fun cancelExpiryNotifications(fileId: String)
