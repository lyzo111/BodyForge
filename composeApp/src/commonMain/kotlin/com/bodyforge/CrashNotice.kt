package com.bodyforge

// What the crash guard has to report on this launch: which settings it reset after repeated
// crashes (if any) and the stack trace of the last crash, so the user can screenshot it.
data class CrashNotice(val resetSettings: String?, val trace: String?)

// Returns the pending notice once and clears it, or null when the last launch was clean.
expect fun consumeCrashNotice(): CrashNotice?
