package com.bodyforge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Fires when a rest timer ends. Vibrates even if the screen is locked or the app process was
// killed, by reading the setting straight from prefs and using the broadcast's own context.
class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("bodyforge_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("vibrate_on_timer_end", true)) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 250, 500, 250, 600)
        // USAGE_ALARM so the buzz plays through Do-Not-Disturb and while the screen is locked.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), attrs)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1, attrs)
        }
    }
}
