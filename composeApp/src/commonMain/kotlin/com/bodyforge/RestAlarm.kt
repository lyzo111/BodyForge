package com.bodyforge

// Schedules / cancels a platform alarm that fires when the current rest ends, so the end buzz
// happens on time even when the app is backgrounded or the phone is locked.
expect fun scheduleRestAlarm(atEpochMillis: Long)
expect fun cancelRestAlarm()

// Shows/cancels a rest-timer notification with a live, system-rendered countdown, so the
// remaining rest time is visible from the notification shade without opening the app.
expect fun showRestNotification(atEpochMillis: Long)
expect fun cancelRestNotification()
