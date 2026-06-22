package com.bodyforge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bodyforge.data.DatabaseFactory

private const val REST_ALARM_REQUEST = 7321

private fun restPendingIntent(ctx: Context): PendingIntent {
    val intent = Intent(ctx, RestAlarmReceiver::class.java)
    return PendingIntent.getBroadcast(
        ctx,
        REST_ALARM_REQUEST,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

actual fun scheduleRestAlarm(atEpochMillis: Long) {
    val ctx = DatabaseFactory.context()
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = restPendingIntent(ctx)
    try {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pi)
    } catch (e: SecurityException) {
        // Exact alarms not permitted by the user; fall back to an inexact, Doze-friendly alarm.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pi)
    }
}

actual fun cancelRestAlarm() {
    val ctx = DatabaseFactory.context()
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(restPendingIntent(ctx))
}
