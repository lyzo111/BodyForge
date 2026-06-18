package com.bodyforge.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Lightweight user settings backed by SharedPreferences so they survive in-place app updates
// without needing a database migration.
object AppSettings {
    private val prefs
        get() = DatabaseFactory.context().getSharedPreferences("bodyforge_settings", Context.MODE_PRIVATE)

    var isolationRestSeconds: Int
        get() = prefs.getInt("isolation_rest", 120)
        set(value) { prefs.edit().putInt("isolation_rest", value).apply() }

    var compoundRestSeconds: Int
        get() = prefs.getInt("compound_rest", 180)
        set(value) { prefs.edit().putInt("compound_rest", value).apply() }

    var vibrateOnTimerEnd: Boolean
        get() = prefs.getBoolean("vibrate_on_timer_end", true)
        set(value) { prefs.edit().putBoolean("vibrate_on_timer_end", value).apply() }
}

// A short vibration pulse, used when the rest timer reaches zero.
fun vibrateDevice(milliseconds: Long = 500) {
    val context = DatabaseFactory.context()
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(milliseconds)
    }
}
