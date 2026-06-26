package com.phonejail.app.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/** "1:23:45" or "04:07" depending on length. Used for the live timer. */
fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = TimeUnit.SECONDS.toHours(totalSeconds)
    val m = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

/** Compact, human-friendly duration for history rows and stats: "2h 14m", "47m", "38s". */
fun formatDurationShort(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}
