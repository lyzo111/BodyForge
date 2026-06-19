package com.bodyforge.ui

import androidx.compose.runtime.Composable

// Handles the platform's system back action (e.g. Android's back button / edge-swipe gesture).
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
