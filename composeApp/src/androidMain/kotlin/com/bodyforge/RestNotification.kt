package com.bodyforge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bodyforge.data.DatabaseFactory

private const val REST_NOTIFICATION_CHANNEL_ID = "rest_timer"
private const val REST_NOTIFICATION_ID = 7322

private fun ensureRestNotificationChannel(manager: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (manager.getNotificationChannel(REST_NOTIFICATION_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        REST_NOTIFICATION_CHANNEL_ID,
        "Rest timer",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Shows the rest timer countdown and lets you know when rest is over"
    }
    manager.createNotificationChannel(channel)
}

private fun openAppPendingIntent(ctx: Context): PendingIntent {
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        ?: Intent(ctx, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun hasNotificationPermission(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

// The channel-aware Notification.Builder(Context, String) constructor requires API 26; minSdk is
// 24, so pre-O devices need the deprecated single-arg constructor instead (channels don't exist
// there, so there is nothing to pass).
private fun newBuilder(ctx: Context): Notification.Builder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(ctx, REST_NOTIFICATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(ctx)
    }

// Live countdown notification: setUsesChronometer + setChronometerCountDown lets the system
// render and tick the remaining time on its own, so no periodic app wake-ups are needed to
// keep it current.
actual fun showRestNotification(atEpochMillis: Long) {
    val ctx = DatabaseFactory.context()
    if (!hasNotificationPermission(ctx)) return
    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    ensureRestNotificationChannel(manager)

    val notification = newBuilder(ctx)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Rest timer")
        .setContentText("Resting — back to it when this hits zero")
        .setContentIntent(openAppPendingIntent(ctx))
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setWhen(atEpochMillis)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_ALARM)
        .build()

    manager.notify(REST_NOTIFICATION_ID, notification)
}

actual fun cancelRestNotification() {
    val ctx = DatabaseFactory.context()
    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(REST_NOTIFICATION_ID)
}

// Called from RestAlarmReceiver when the rest period actually ends, so the live countdown
// notification is replaced with a plain "time's up" one instead of just disappearing.
internal fun postRestCompleteNotification(ctx: Context) {
    if (!hasNotificationPermission(ctx)) return
    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    ensureRestNotificationChannel(manager)

    val notification = newBuilder(ctx)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Rest complete")
        .setContentText("Back to it 💪")
        .setContentIntent(openAppPendingIntent(ctx))
        .setOngoing(false)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_ALARM)
        .build()

    manager.notify(REST_NOTIFICATION_ID, notification)
}
