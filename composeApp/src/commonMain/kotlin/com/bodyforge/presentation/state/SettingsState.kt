package com.bodyforge.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bodyforge.data.AppSettings

// Compose-observable mirror of the settings that affect live screens, so flipping a setting
// recomposes the relevant UI immediately instead of on the next incidental redraw. AppSettings stays
// the persistent source of truth; this is refreshed from it whenever the Settings dialog saves.
object SettingsState {
    var editCompletedSets by mutableStateOf(AppSettings.editCompletedSets)
        private set
    var useLbs by mutableStateOf(AppSettings.useLbs)
        private set

    fun reload() {
        editCompletedSets = AppSettings.editCompletedSets
        useLbs = AppSettings.useLbs
    }
}
