package com.bodyforge.ui

import androidx.compose.runtime.Composable

// Remembers a launcher that opens the system file picker for a CSV and hands back its text content.
@Composable
expect fun rememberCsvImporter(onCsv: (String) -> Unit): () -> Unit
