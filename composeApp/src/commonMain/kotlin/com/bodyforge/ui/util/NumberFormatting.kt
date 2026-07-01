package com.bodyforge.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Locale-aware thousands separator for large numbers (e.g. total volume).
 * Uses the device locale so it reads "44.883" on a German device and
 * "44,883" on an English one, matching the date formatting already used
 * elsewhere in the app (SimpleDateFormat(..., Locale.getDefault())).
 */
fun formatThousands(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

fun formatThousands(value: Double): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.roundToInt())
