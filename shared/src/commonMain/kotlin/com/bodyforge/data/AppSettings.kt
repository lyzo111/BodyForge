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

    // When false (default), a set can no longer be edited once it is marked done; when true,
    // completed sets stay editable.
    var editCompletedSets: Boolean
        get() = prefs.getBoolean("edit_completed_sets", false)
        set(value) { prefs.edit().putBoolean("edit_completed_sets", value).apply() }

    // Display unit for weights. Stored data stays in kilograms; this only changes how weights are
    // shown and entered across the app.
    var useLbs: Boolean
        get() = prefs.getBoolean("use_lbs", false)
        set(value) { prefs.edit().putBoolean("use_lbs", value).apply() }

    // When true (default), decorative emojis are shown; when false they are replaced with icons.
    var emojiMode: Boolean
        get() = prefs.getBoolean("emoji_mode", true)
        set(value) { prefs.edit().putBoolean("emoji_mode", value).apply() }

    // templateId -> split name (e.g. "PPL"). Persisted here, so splits need no database migration.
    // Entries are joined with control characters (record/unit separators) that users won't type.
    private const val RECORD_SEP = "\u001E"
    private const val UNIT_SEP = "\u001F"

    var splitAssignments: Map<String, String>
        get() {
            val raw = prefs.getString("split_assignments", "") ?: ""
            if (raw.isEmpty()) return emptyMap()
            return raw.split(RECORD_SEP).mapNotNull { entry ->
                val parts = entry.split(UNIT_SEP)
                if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
            }.toMap()
        }
        set(value) {
            val raw = value.entries.joinToString(RECORD_SEP) { "${it.key}$UNIT_SEP${it.value}" }
            prefs.edit().putString("split_assignments", raw).apply()
        }

    // phaseId -> split name used during that training phase (e.g. "PPL"). Persisted alongside the
    // template split assignments, so linking a phase to a split needs no database migration.
    var phaseSplits: Map<String, String>
        get() {
            val raw = prefs.getString("phase_splits", "") ?: ""
            if (raw.isEmpty()) return emptyMap()
            return raw.split(RECORD_SEP).mapNotNull { entry ->
                val parts = entry.split(UNIT_SEP)
                if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
            }.toMap()
        }
        set(value) {
            val raw = value.entries.joinToString(RECORD_SEP) { "${it.key}$UNIT_SEP${it.value}" }
            prefs.edit().putString("phase_splits", raw).apply()
        }

    // Whether the Templates list groups by split (true) or routine (false). Remembered across launches.
    var groupTemplatesBySplit: Boolean
        get() = prefs.getBoolean("group_templates_by_split", false)
        set(value) { prefs.edit().putBoolean("group_templates_by_split", value).apply() }

    // Name of the selected colour theme (see com.bodyforge.ui.theme). Defaults to the first theme.
    var themeName: String
        get() = prefs.getString("theme_name", "Midnight") ?: "Midnight"
        set(value) { prefs.edit().putString("theme_name", value).apply() }
}

// A noticeable vibration pattern, used when the rest timer reaches zero. Several pulses so it's
// hard to miss even with the phone pocketed between sets.
fun vibrateDevice() {
    val context = DatabaseFactory.context()
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    // Wait, buzz, pause, buzz, pause, buzz.
    val pattern = longArrayOf(0, 500, 250, 500, 250, 600)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}
