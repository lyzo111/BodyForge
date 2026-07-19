package com.bodyforge

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.bodyforge.data.DatabaseFactory

// Crash recovery: every uncaught exception bumps a persisted streak counter and stores its stack
// trace. Three crashes in a row reset the most recently changed setting (or, if unknown, all
// behavior settings) back to defaults, so a setting that breaks startup can't lock the app.
// Surviving the first seconds of a launch ends the streak.
object CrashGuard {
    private const val PREFS = "bodyforge_crash_guard"
    private const val KEY_STREAK = "crash_streak"
    private const val KEY_LAST_CRASH = "last_crash"
    private const val KEY_RESET_NOTICE = "reset_notice"
    private const val CRASH_STREAK_LIMIT = 3
    private const val ALIVE_AFTER_MS = 8000L

    private const val SETTINGS_PREFS = "bodyforge_settings"
    private const val KEY_LAST_CHANGED_SETTING = "last_changed_setting"
    private val BEHAVIOR_SETTING_KEYS = listOf(
        "big_button_mode", "emoji_mode", "use_lbs", "edit_completed_sets",
        "vibrate_on_timer_end", "isolation_rest", "compound_rest", "theme_name", "weight_step"
    )

    private fun guardPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun install(context: Context) {
        val appContext = context.applicationContext
        val prefs = guardPrefs(appContext)

        if (prefs.getInt(KEY_STREAK, 0) >= CRASH_STREAK_LIMIT) {
            resetSuspectSettings(appContext, prefs)
        }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // commit(), not apply(): the process dies right after this handler returns.
                prefs.edit()
                    .putInt(KEY_STREAK, prefs.getInt(KEY_STREAK, 0) + 1)
                    .putString(KEY_LAST_CRASH, throwable.stackTraceToString().take(4000))
                    .commit()
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }

        // Staying alive this long counts as a successful launch and ends the streak.
        Handler(Looper.getMainLooper()).postDelayed(
            { prefs.edit().putInt(KEY_STREAK, 0).apply() },
            ALIVE_AFTER_MS
        )
    }

    private fun resetSuspectSettings(context: Context, guardPrefs: SharedPreferences) {
        val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val lastChanged = settings.getString(KEY_LAST_CHANGED_SETTING, null)
            ?.takeIf { it in BEHAVIOR_SETTING_KEYS }
        val resetKeys = lastChanged?.let { listOf(it) } ?: BEHAVIOR_SETTING_KEYS
        val editor = settings.edit()
        // Removing a key restores its default, since every getter carries one.
        resetKeys.forEach { editor.remove(it) }
        editor.remove(KEY_LAST_CHANGED_SETTING)
        editor.commit()
        guardPrefs.edit()
            .putInt(KEY_STREAK, 0)
            .putString(KEY_RESET_NOTICE, resetKeys.joinToString(", ") { settingDisplayName(it) })
            .commit()
    }

    private fun settingDisplayName(key: String): String = when (key) {
        "big_button_mode" -> "Big Button Mode"
        "emoji_mode" -> "Emoji mode"
        "use_lbs" -> "Weight unit"
        "edit_completed_sets" -> "Edit sets after completing"
        "vibrate_on_timer_end" -> "Vibrate when timer ends"
        "isolation_rest" -> "Isolation rest"
        "compound_rest" -> "Compound rest"
        "theme_name" -> "Theme"
        "weight_step" -> "Weight step"
        else -> key
    }
}

actual fun consumeCrashNotice(): CrashNotice? {
    val prefs = DatabaseFactory.context()
        .getSharedPreferences("bodyforge_crash_guard", Context.MODE_PRIVATE)
    val trace = prefs.getString("last_crash", null)
    val reset = prefs.getString("reset_notice", null)
    if (trace == null && reset == null) return null
    prefs.edit().remove("last_crash").remove("reset_notice").apply()
    return CrashNotice(resetSettings = reset, trace = trace)
}
