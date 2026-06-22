package com.bodyforge

// Schedules / cancels a platform alarm that fires when the current rest ends, so the end buzz
// happens on time even when the app is backgrounded or the phone is locked.
expect fun scheduleRestAlarm(atEpochMillis: Long)
expect fun cancelRestAlarm()
