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

    // Name of the most recently changed user-facing setting. The crash guard resets exactly this
    // one to its default when the app crashes three times in a row.
    private const val LAST_CHANGED = "last_changed_setting"

    var isolationRestSeconds: Int
        get() = prefs.getInt("isolation_rest", 120)
        set(value) { prefs.edit().putInt("isolation_rest", value).putString(LAST_CHANGED, "isolation_rest").apply() }

    var compoundRestSeconds: Int
        get() = prefs.getInt("compound_rest", 180)
        set(value) { prefs.edit().putInt("compound_rest", value).putString(LAST_CHANGED, "compound_rest").apply() }

    var vibrateOnTimerEnd: Boolean
        get() = prefs.getBoolean("vibrate_on_timer_end", true)
        set(value) { prefs.edit().putBoolean("vibrate_on_timer_end", value).putString(LAST_CHANGED, "vibrate_on_timer_end").apply() }

    // When false (default), a set can no longer be edited once it is marked done; when true,
    // completed sets stay editable.
    var editCompletedSets: Boolean
        get() = prefs.getBoolean("edit_completed_sets", false)
        set(value) { prefs.edit().putBoolean("edit_completed_sets", value).putString(LAST_CHANGED, "edit_completed_sets").apply() }

    // Display unit for weights. Stored data stays in kilograms; this only changes how weights are
    // shown and entered across the app.
    var useLbs: Boolean
        get() = prefs.getBoolean("use_lbs", false)
        set(value) { prefs.edit().putBoolean("use_lbs", value).putString(LAST_CHANGED, "use_lbs").apply() }

    // When true (default), decorative emojis are shown; when false they are replaced with icons.
    var emojiMode: Boolean
        get() = prefs.getBoolean("emoji_mode", true)
        set(value) { prefs.edit().putBoolean("emoji_mode", value).putString(LAST_CHANGED, "emoji_mode").apply() }

    // When true, the active workout uses the old large stacked per-set buttons; when false
    // (default) it uses the compact one-row-per-set layout.
    var bigButtonMode: Boolean
        get() = prefs.getBoolean("big_button_mode", false)
        set(value) { prefs.edit().putBoolean("big_button_mode", value).putString(LAST_CHANGED, "big_button_mode").apply() }

    // Step applied by the +/− weight buttons in the active workout and the history editor, in the
    // current display unit (kg or lbs).
    var weightStep: Double
        get() = prefs.getFloat("weight_step", 2.5f).toDouble()
        set(value) { prefs.edit().putFloat("weight_step", value.toFloat()).putString(LAST_CHANGED, "weight_step").apply() }

    // How a template-started workout prefills from history. When true (default), the set count /
    // the reps & weight come from the last session of the SAME template variation (e.g. last
    // "Pull A"); when false, from the most recent session of that exercise in any variation.
    var prefillSetsSameVariation: Boolean
        get() = prefs.getBoolean("prefill_sets_same_variation", true)
        set(value) { prefs.edit().putBoolean("prefill_sets_same_variation", value).putString(LAST_CHANGED, "prefill_sets_same_variation").apply() }

    var prefillWeightsSameVariation: Boolean
        get() = prefs.getBoolean("prefill_weights_same_variation", true)
        set(value) { prefs.edit().putBoolean("prefill_weights_same_variation", value).putString(LAST_CHANGED, "prefill_weights_same_variation").apply() }

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

    // Which exercise cards are collapsed in the active workout, so the fold state survives app
    // restarts. Scoped to one workout id — a different workout starts fully expanded again.
    var collapsedWorkoutId: String
        get() = prefs.getString("collapsed_workout_id", "") ?: ""
        set(value) { prefs.edit().putString("collapsed_workout_id", value).apply() }

    var collapsedExerciseIds: Set<String>
        get() {
            val raw = prefs.getString("collapsed_exercise_ids", "") ?: ""
            return if (raw.isEmpty()) emptySet() else raw.split(RECORD_SEP).toSet()
        }
        set(value) { prefs.edit().putString("collapsed_exercise_ids", value.joinToString(RECORD_SEP)).apply() }

    // Split rotation: which split is being rotated through, the template order of one full cycle,
    // and a forever-incrementing pointer to the next suggestion (consumers take it modulo).
    var rotationSplit: String
        get() = prefs.getString("rotation_split", "") ?: ""
        set(value) { prefs.edit().putString("rotation_split", value).apply() }

    var rotationOrder: List<String>
        get() {
            val raw = prefs.getString("rotation_order", "") ?: ""
            return if (raw.isEmpty()) emptyList() else raw.split(RECORD_SEP)
        }
        set(value) { prefs.edit().putString("rotation_order", value.joinToString(RECORD_SEP)).apply() }

    var rotationIndex: Int
        get() = prefs.getInt("rotation_index", 0)
        set(value) { prefs.edit().putInt("rotation_index", value).apply() }

    // Duplicate-exercise pairs (as "idA|idB", ids sorted) the user has already been asked about
    // merging, so the startup prompt never repeats a decision.
    var duplicateMergeAsked: Set<String>
        get() {
            val raw = prefs.getString("duplicate_merge_asked", "") ?: ""
            return if (raw.isEmpty()) emptySet() else raw.split(RECORD_SEP).toSet()
        }
        set(value) { prefs.edit().putString("duplicate_merge_asked", value.joinToString(RECORD_SEP)).apply() }

    // Name of the selected colour theme (see com.bodyforge.ui.theme). Defaults to the first theme.
    var themeName: String
        get() = prefs.getString("theme_name", "Midnight") ?: "Midnight"
        set(value) { prefs.edit().putString("theme_name", value).putString(LAST_CHANGED, "theme_name").apply() }
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
