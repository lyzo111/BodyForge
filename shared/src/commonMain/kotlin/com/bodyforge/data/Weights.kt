package com.bodyforge.data

import kotlin.math.roundToInt

// Weights are always stored in kilograms. This central helper converts to and from the user's
// chosen display unit (kg or lbs, from AppSettings) so every screen can show and accept weights in
// that unit without duplicating the maths.
object Weights {
    private const val LB_PER_KG = 2.2046226218

    // composeApp installs a reactive provider so weight displays recompose when the unit changes;
    // without it (e.g. tests) we fall back to the persisted setting.
    var useLbsProvider: (() -> Boolean)? = null
    val useLbs: Boolean get() = useLbsProvider?.invoke() ?: AppSettings.useLbs

    // Short label for the active display unit.
    val unit: String get() = if (useLbs) "lbs" else "kg"

    // A stored kilogram value expressed in the display unit.
    fun toDisplay(kg: Double): Double = if (useLbs) kg * LB_PER_KG else kg

    // A value the user typed (in the display unit) converted back to kilograms for storage.
    fun toKg(displayValue: Double): Double = if (useLbs) displayValue / LB_PER_KG else displayValue

    // Display-unit number without a trailing ".0"; otherwise rounded to one decimal place.
    fun format(kg: Double): String {
        val v = toDisplay(kg)
        return if (v % 1.0 == 0.0) v.toInt().toString() else ((v * 10).roundToInt() / 10.0).toString()
    }

    // Display-unit number with the unit label, e.g. "82.5 lbs".
    fun formatWithUnit(kg: Double): String = "${format(kg)} $unit"

    // Volumes and other large totals read better as a whole number in the display unit.
    fun formatRounded(kg: Double): String = toDisplay(kg).roundToInt().toString()
}
